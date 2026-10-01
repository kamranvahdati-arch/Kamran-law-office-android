package ir.kamranvahdati.lawoffice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Versioned independent legal content boundary; no website database or HTML dependency. */
public interface LegalContentApi {
    String CONTRACT_VERSION = "v1";
    enum Kind { LAW, ARTICLE, UNIFICATION_RULING, ADVISORY_OPINION }
    void search(Query query, Callback<Page> callback);
    void get(String contentId, Callback<Entry> callback);
    interface Callback<T> {
        void success(T value);
        void unavailable(String message);
    }
    final class Query {
        public final String search, category, cursor;
        public final Kind kind;
        public final int pageSize;
        public Query(String search, String category, Kind kind, String cursor, int pageSize) {
            if (pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("page size");
            this.search = search == null ? "" : search.trim();
            this.category = category; this.kind = kind; this.cursor = cursor; this.pageSize = pageSize;
        }
    }
    final class Entry {
        public final String id, title, body, officialSourceUrl, sourceDate, revision;
        public final Kind kind;
        public Entry(String id, String title, String body, String officialSourceUrl,
                     String sourceDate, String revision, Kind kind) {
            this.id = required(id); this.title = required(title); this.body = required(body);
            this.officialSourceUrl = required(officialSourceUrl);
            if (!officialSourceUrl.startsWith("https://")) throw new IllegalArgumentException("source URL");
            this.sourceDate = required(sourceDate); this.revision = required(revision);
            if (kind == null) throw new IllegalArgumentException("kind");
            this.kind = kind;
        }
        private static String required(String value) {
            if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("content field");
            return value;
        }
    }
    final class Page {
        public final List<Entry> entries;
        public final String nextCursor;
        public Page(List<Entry> entries, String nextCursor) {
            if (entries == null || entries.contains(null)) throw new IllegalArgumentException("entries");
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
            this.nextCursor = nextCursor;
        }
    }
    final class Unavailable implements LegalContentApi {
        public void search(Query query, Callback<Page> callback) { callback.unavailable("در حال آماده‌سازی اتصال به پایگاه حقوقی"); }
        public void get(String contentId, Callback<Entry> callback) { callback.unavailable("در حال آماده‌سازی اتصال به پایگاه حقوقی"); }
    }
}
