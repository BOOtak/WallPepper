package org.catinthedark.wallpepper.json;

/**
 * Created by kirill on 21.09.14.
 */
public class JsonHelpers {
    public class RecentPhotosResponse {
        public PhotoPageInfo photos;
        public String stat;
    }

    public class PhotoPageInfo {
        public int page;
        public int pages;
        public int perpage;
        public String total;
        public Photo[] photo;
    }

    public class Photo {
        public String id;
        public String owner;
        public String sercet;
        public String server;
        public int farm;
        public String title;
        public int ispublic;
        public int isfriend;
        public int isfamily;
    }

    public class ImageSizesResponse {
        public Sizes sizes;
        public String stat;
    }

    public class Sizes {
        public Size[] size;
    }

    public class Size {
        public String label;
        public String width;
        public String height;
        public String source;
        public String url;
        public String media;
    }
}
