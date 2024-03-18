import io.nayuki.fastqrcodegen.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import java.util.zip.*;
import java.nio.*;

import com.google.common.collect.MinMaxPriorityQueue;
import java.util.concurrent.*;

// ECC Q can tolerate 6 blocks
// ECC H can tolerate 8 blocks
class ThreadResult {
    ThreadResult(MinMaxPriorityQueue<QrCode> edgeHeap, MinMaxPriorityQueue<QrCode> countHeap, long iters) {
        this.edgeHeap = edgeHeap;
        this.countHeap = countHeap;
        this.iters = iters;
    }
    public MinMaxPriorityQueue<QrCode> edgeHeap, countHeap;
    public long iters;
}
public class Main {
    public static void main(String[] args) throws IOException {
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numCores);

        List<Future<ThreadResult>> futures = new ArrayList<>(); // Holds futures that can be used to get results of tasks
        long start = System.nanoTime();
        for (int i = 0; i < numCores; i++) {
            final int _i = i;

            Callable<ThreadResult> task = new Callable<ThreadResult>() {
                @Override
                public ThreadResult call() {
                    MinMaxPriorityQueue<QrCode> edgeHeap = MinMaxPriorityQueue.orderedBy(Comparator.comparing(QrCode::getEdgeScore))
                            .maximumSize(100).create();
                    MinMaxPriorityQueue<QrCode> countHeap = MinMaxPriorityQueue.orderedBy(Comparator.comparing(QrCode::getCountScore))
                            .maximumSize(100).create();
                    PermutationGenerator permGen = new PermutationGenerator(3, 4, 1, numCores, _i);

                    long iters = permGen.URLStream().peek(url -> {
                        QrCode qrTemplate = QrCode.computeAllCodewords(QrSegment.makeSegments(url),
                                QrCode.Ecc.LOW, 1, true);
                        qrTemplate.text = url;
                        for (int mask = 0; mask < 8; mask++) {
                            QrCode qr = qrTemplate.clone();
                            qr.finalizeMask(mask);
                            edgeHeap.add(qr);
                            countHeap.add(qr);
                        }
                    }).count();
                    return new ThreadResult(edgeHeap, countHeap, iters);
                }
            };

            // Submit the task to the executor. This starts the task running and returns a Future that can be used to get the result
            Future<ThreadResult> future = executor.submit(task);

            // Add the future to the list of futures
            futures.add(future);
        }

        MinMaxPriorityQueue<QrCode> edgeHeap = MinMaxPriorityQueue.orderedBy(Comparator.comparing(QrCode::getEdgeScore))
                .maximumSize(100).create();
        MinMaxPriorityQueue<QrCode> countHeap = MinMaxPriorityQueue.orderedBy(Comparator.comparing(QrCode::getCountScore))
                .maximumSize(100).create();
        long iters = 0;

        // Now retrieve the result of each task. This is done in the main thread, combining the subresults.
//        int completedJobs = 0;
        for (Future<ThreadResult> future : futures) {
            try {
                ThreadResult result = future.get(); // This will block until the result is available
                edgeHeap.addAll(result.edgeHeap);
                countHeap.addAll(result.countHeap);
                iters += result.iters;
//                completedJobs++;
//                QrCode best = edgeHeap.peek();
//                System.out.printf("%d/%d domains complete, top score: %d (%s) %n", completedJobs, futures.size(), best.score, best.text);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Shutdown the executor service
        executor.shutdown();

        long finish = System.nanoTime();
        double timeElapsed = (double)(finish - start) / 1000000000;

        System.out.printf("Elapsed: %fs, Iters: %d, qrs/s: %d%n", timeElapsed, iters, (int)(iters/timeElapsed));
        int i = 1;
        System.out.println("Ranking by edges:");
        while (!edgeHeap.isEmpty()) {
            QrCode qr = edgeHeap.poll();
            System.out.printf("#%d %s\t(%d, mask %d, ECC %s, skips: %s)%n",
                    i, qr.text, qr.edgeScore, qr.mask, qr.errorCorrectionLevel, Arrays.toString(qr.edgeSkipBlocks));
            BufferedImage img = toImage(qr, qr.edgeSkipBlocks);
            ImageIO.write(img, "png", new File("E:\\qrs_edges\\" + i + ".png"));
            i++;
        }
        i = 1;
        System.out.println("Ranking by count:");
        while (!countHeap.isEmpty()) {
            QrCode qr = countHeap.poll();
            System.out.printf("#%d %s\t(%d, mask %d, ECC %s, skips: %s)%n",
                    i, qr.text, qr.countScore, qr.mask, qr.errorCorrectionLevel, Arrays.toString(qr.countSkipBlocks));
            BufferedImage img = toImage(qr, qr.countSkipBlocks);
            ImageIO.write(img, "png", new File("E:\\qrs_counts\\" + i + ".png"));
            i++;
        }
    }

    static private int deflateLength(byte[] bytes) {
        byte[] tmpOutput = new byte[256];
        Deflater c1 = new Deflater();
        c1.setInput(bytes);
        c1.finish();
        int c1_len = c1.deflate(tmpOutput);
        c1.end();
        return c1_len;
    }

    static private byte[] intArrToBytes(int[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);
        return byteBuffer.array();
    }

    private static BufferedImage toImage(QrCode qr, int[] skipBlocks) {
        BufferedImage result = new BufferedImage((qr.size + 4 * 2) * 10, (qr.size + 4 * 2) * 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                result.setRGB(x, y, 0xffffff);
            }
        }
        int[] colorArray = new int[100];
        for (int y = 0; y < 21; y++) {
            for (int x = 0; x < 21; x++) {
                int block_idx = QrCode.BLOCK_COORDS_TO_IDX[x + y * 21];
//                System.out.println(block_idx);
                List<Integer> suggestedBlocks = Arrays.stream(skipBlocks).boxed().toList();
                boolean b = qr.getModule(x, y);
                int color = b ? 0 : 0xffffff;
                if (block_idx != -1 && suggestedBlocks.contains(block_idx)) {
                    color = b ? 0x330000 : 0xffcccc;
                }
                Arrays.fill(colorArray, color);
                result.setRGB((x + 4) * 10, (y + 4) * 10, 10, 10, colorArray, 0, 1);
            }
        }
        return result;
    }
}