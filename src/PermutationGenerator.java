import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PermutationGenerator {

    PermutationGenerator(int len) {
        this(len, len, len, 1, 0);
    }

    PermutationGenerator(int min_domain_len, int domain_len, int path_len, int step, int start) {
        this.min_domain_len = min_domain_len;
        for (int i = min_domain_len; i <= domain_len; i++) {
            this.max_domain_perms += (long) Math.pow(ALPHABET.length, i);
        }

        for (int i = 0; i <= path_len; i++) {
            this.max_path_perms += (long) Math.pow(PATH_CHARS.length, i);
        }
        this.max_path_perms++;
        this.start = start;
        this.step = step;
    }

    Stream<String> URLStream() {
        return domainStream().flatMap(domain -> {
            // System.out.println(domain);
            return coreTldStream().map(tld ->
                    domain + "." + tld
            );
            /*
            return tldStream().flatMap(tld ->
                    Arrays.stream(CORE_TLDS_SET.contains(tld) ? CORE_TLD_SCHEMES : SCHEMES).flatMap(scheme ->
                            pathStream().map(path ->
                                    scheme + domain + "." + tld + path
                            )
                    )
            );
            */
        }).filter(url -> url.length() <= maxlen);
    }

    public Stream<String> domainStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
            long count = start;

            @Override
            public boolean hasNext() {
                return count < max_domain_perms;
            }

            @Override
            public String next() {
                StringBuilder combination = new StringBuilder();
                long tempCount = count;
                int tempCombinationLength = min_domain_len;
                while (tempCount >= Math.pow(ALPHABET.length, tempCombinationLength)) {
                    tempCount -= (long) Math.pow(ALPHABET.length, tempCombinationLength);
                    tempCombinationLength++;
                }
                for (int i = 0; i < tempCombinationLength; i++) {
                    int index = (int) (tempCount % ALPHABET.length);
                    combination.append(ALPHABET[index]);
                    tempCount /= ALPHABET.length;
                }
                count += step;
                return combination.toString();
            }
        }, Spliterator.IMMUTABLE), false)
        .filter((String s) -> s.charAt(s.length() - 1) != '-' && s.charAt(0) != '-');
    }

    public Stream<String> tldStream() {
        return Arrays.stream(TLDS);
    }

    public Stream<String> coreTldStream() {
        return Arrays.stream(CORE_TLDS);
    }

    static String[] CORE_TLD_SCHEMES = {"", "http://", "https://", "www."};
    static String[] SCHEMES = {"http://", "https://", "www."};

    public Stream<String> pathStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
            long count = 0;

            @Override
            public boolean hasNext() {
                return count < max_path_perms;
            }

            @Override
            public String next() {
                StringBuilder combination = new StringBuilder("/");
                long tempCount = count;
                int tempCombinationLength = 0;
                while (tempCount >= Math.pow(PATH_CHARS.length, tempCombinationLength)) {
                    tempCount -= (long) Math.pow(PATH_CHARS.length, tempCombinationLength);
                    tempCombinationLength++;
                }
                for (int i = 0; i < tempCombinationLength; i++) {
                    int index = (int) (tempCount % PATH_CHARS.length);
                    combination.append(PATH_CHARS[index]);
                    tempCount /= PATH_CHARS.length;
                }
                count++;
                if (count == max_path_perms) return "";
                return combination.toString();
            }
        }, Spliterator.IMMUTABLE), false);
    }

    public static List<String> generatePrefixes(int len) {
        ArrayList<String> ret = new ArrayList<String>();
        for (int num = (int) Math.pow(ALPHABET.length, len) - 1; num >= 0; num--) {
            int tmp = num;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < len; i++) {
                builder.append(ALPHABET[tmp % ALPHABET.length]);
                tmp /= ALPHABET.length;
            }
            if (builder.charAt(0) == '-') continue;
            ret.add(builder.toString());
        }
        return ret;
    }

    static <T> T concatWithCopy(T array1, T array2) {
        if (!array1.getClass().isArray() || !array2.getClass().isArray()) {
            throw new IllegalArgumentException("Only arrays are accepted.");
        }

        Class<?> compType1 = array1.getClass().getComponentType();
        Class<?> compType2 = array2.getClass().getComponentType();

        if (!compType1.equals(compType2)) {
            throw new IllegalArgumentException("Two arrays have different types.");
        }

        int len1 = Array.getLength(array1);
        int len2 = Array.getLength(array2);

        @SuppressWarnings("unchecked")
        //the cast is safe due to the previous checks
        T result = (T) Array.newInstance(compType1, len1 + len2);

        System.arraycopy(array1, 0, result, 0, len1);
        System.arraycopy(array2, 0, result, len1, len2);

        return result;
    }

    static String[] TLDS ={"ac", "academy", "accountant", "accountants", "actor", "ads", "adult", "africa", "ag", "agency", "ai", "airforce", "alsace", "am", "amsterdam", "analytics", "apartments", "app", "ar", "arab", "archi", "army", "art", "asia", "associates", "at", "attorney", "au", "auction", "audio", "author", "auto", "autos", "baby", "band", "bank", "bar", "barcelona", "bargains", "baseball", "basketball", "bayern", "bcn", "be", "beauty", "beer", "berlin", "best", "bet", "bible", "bid", "bike", "bingo", "bio", "biz", "black", "blackfriday", "blog", "blue", "boats", "bond", "boo", "book", "boston", "bot", "boutique", "box", "broadway", "broker", "brussels", "budapest", "build", "builders", "business", "buy", "buzz", "bz", "bzh", "ca", "cab", "cafe", "call", "cam", "camera", "camp", "cancerresearch", "capetown", "capital", "car", "cards", "care", "career", "careers", "cars", "casa", "cash", "casino", "catering", "catholic", "cc", "center", "ceo", "cfd", "ch", "channel", "charity", "chat", "cheap", "christmas", "church", "city", "cl", "claims", "cleaning", "click", "clinic", "clothing", "cloud", "club", "cm", "cn", "co", "coach", "codes", "coffee", "college", "cologne", "com", "community", "company", "compare", "computer", "comsec", "condos", "construction", "consulting", "contact", "contractors", "cooking", "cool", "corp", "country", "coupon", "coupons", "courses", "cpa", "credit", "creditcard", "creditunion", "cricket", "cruise", "cruises", "cx", "cymru", "cz", "dad", "dance", "data", "date", "dating", "day", "dds", "de", "deal", "dealer", "deals", "degree", "delivery", "democrat", "dental", "dentist", "desi", "design", "dev", "diamonds", "diet", "digital", "direct", "directory", "discount", "diy", "dk", "docs", "doctor", "dog", "domains", "dot", "download", "dubai", "dvr", "earth", "eat", "ec", "ecom", "education", "email", "energy", "engineer", "engineering", "enterprises", "equipment", "es", "esq", "estate", "eu", "events", "exchange", "expert", "exposed", "express", "fail", "faith", "family", "fan", "fans", "farm", "fashion", "feedback", "film", "final", "finance", "financial", "finish", "fish", "fishing", "fit", "fitness", "flights", "florist", "flowers", "fly", "fm", "foo", "food", "football", "forex", "forsale", "forum", "foundation", "fr", "frl", "fun", "fund", "furniture", "futbol", "fyi", "gal", "gallery", "game", "games", "garden", "gay", "ged", "gent", "gg", "gift", "gifts", "gives", "giving", "glass", "global", "gmbh", "gold", "golf", "got", "gr", "graphics", "gratis", "green", "gripe", "grocery", "group", "gs", "guide", "guitars", "guru", "gy", "hair", "halal", "hamburg", "haus", "health", "healthcare", "help", "helsinki", "here", "hiphop", "hiv", "hk", "hn", "hockey", "holdings", "holiday", "home", "homes", "horse", "hospital", "host", "hosting", "hot", "hoteis", "hotel", "hotels", "house", "how", "hu", "icu", "idn", "im", "immo", "immobilien", "in", "inc", "industries", "info", "ing", "ink", "institute", "insurance", "insure", "international", "investments", "io", "irish", "is", "islam", "ismaili", "ist", "istanbul", "it", "jetzt", "jewelry", "jobs", "joburg", "jot", "joy", "jp", "juegos", "kaufen", "ki", "kid", "kids", "kim", "kitchen", "kiwi", "koeln", "kosher", "kr", "ky", "kyoto", "la", "land", "lat", "latino", "law", "lawyer", "lc", "lease", "legal", "lgbt", "li", "life", "lifeinsurance", "lifestyle", "lighting", "like", "limited", "limo", "link", "live", "living", "llc", "llp", "loan", "loans", "lol", "london", "love", "ltd", "ltda", "luxe", "luxury", "ly", "madrid", "mail", "maison", "makeup", "management", "map", "market", "marketing", "markets", "mba", "me", "med", "media", "medical", "meet", "melbourne", "meme", "memorial", "men", "menu", "miami", "mls", "mn", "mobi", "mobile", "moda", "moe", "mom", "money", "monster", "mortgage", "moscow", "moto", "motorcycles", "mov", "movie", "ms", "music", "mutualfunds", "mx", "nagoya", "name", "navy", "net", "network", "new", "news", "nexus", "nf", "ninja", "nl", "no", "now", "nrw", "nyc", "nz", "observer", "okinawa", "one", "onl", "online", "ooo", "org", "organic", "osaka", "ott", "page", "paris", "pars", "partners", "parts", "party", "pay", "pe", "pet", "pets", "ph", "phd", "phone", "photo", "photography", "photos", "pics", "pictures", "pid", "ping", "pink", "pizza", "pl", "place", "play", "plumbing", "plus", "poker", "politie", "porn", "press", "pro", "productions", "prof", "promo", "properties", "property", "protection", "pt", "pub", "pw", "qpon", "quebec", "quest", "racing", "radio", "read", "realestate", "realtor", "realty", "recipes", "red", "rehab", "reise", "reisen", "rent", "rentals", "repair", "report", "republican", "rest", "restaurant", "retirement", "review", "reviews", "rich", "rip", "rocks", "rodeo", "roma", "room", "rs", "rsvp", "ru", "rugby", "ruhr", "run", "ryukyu", "saarland", "safe", "safety", "sale", "salon", "sari", "sarl", "save", "sb", "sbs", "sc", "scholarships", "school", "schule", "science", "se", "search", "secure", "security", "select", "services", "sex", "sexy", "sg", "sh", "shabaka", "shia", "shiksha", "shoes", "shop", "shopping", "show", "si", "singles", "site", "ski", "skin", "smile", "so", "soccer", "social", "software", "solar", "solutions", "song", "soy", "spa", "space", "sport", "sports", "spot", "srl", "stockholm", "storage", "store", "stream", "studio", "study", "style", "sucks", "supplies", "supply", "support", "surf", "surgery", "swiss", "sydney", "systems", "taipei", "talk", "tatar", "tattoo", "tax", "taxi", "team", "tech", "technology", "tel", "tennis", "thai", "theater", "theatre", "tickets", "tienda", "tips", "tires", "tirol", "tl", "tn", "today", "tokyo", "tools", "top", "tours", "town", "toys", "trade", "trading", "training", "travel", "trust", "tube", "tv", "tw", "uk", "university", "uno", "us", "vacations", "vc", "vegas", "ventures", "versicherung", "vet", "viajes", "video", "villas", "vin", "vip", "vision", "vlaanderen", "vodka", "vote", "voting", "voto", "voyage", "vu", "wales", "wang", "watch", "watches", "weather", "web", "webcam", "webs", "website", "wedding", "wiki", "win", "wine", "winners", "work", "works", "world", "wow", "ws", "wtf", "xxx", "xyz", "yachts", "yoga", "yokohama", "you", "zero", "zip", "zone"};
    static String[] CORE_TLDS = {"ar", "at", "au", "cc", "ch", "cn", "co", "com", "de", "dk", "es", "eu", "fm", "fr", "gr", "hk", "hu", "info", "io", "is", "it", "kr", "ly", "me", "mx", "net", "nl", "no", "nz", "org", "pt", "pw", "rs", "ru", "se", "sg", "si", "tn", "tw",  "uk", "us"};
    static Set<String> CORE_TLDS_SET = new HashSet<>(Arrays.asList(CORE_TLDS));
    static char[] ALPHABET = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-'};
    static char[] ADDITIONAL_PATH_CHARS = {'_', '~', '!', '$', '&' ,'\'', '(', ')', '*', '+', ',', ';', '=', ':', '@'};
    static char[] PATH_CHARS = concatWithCopy(ALPHABET, ADDITIONAL_PATH_CHARS);

    private final int step;
    private final int start;
    public long domain_idx = 0, max_domain_perms = 0;
    public int min_domain_len;

    public long path_idx = 0, max_path_perms = 0;
    public int tld_idx = 0, maxlen = 7;
}