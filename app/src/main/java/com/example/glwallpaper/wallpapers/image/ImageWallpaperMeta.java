package com.example.glwallpaper.wallpapers.image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * On 2021-11-23
 */
public class ImageWallpaperMeta {

    public static final int DEFAULT_MOVE_DISTANCE = 20;
    public static final float DEFAULT_EXTRA_SCALE = 0.2f;

    public List<String> images;
    public List<Float> moveFactors;
    public int moveDistance = DEFAULT_MOVE_DISTANCE;
    public float extraScale = DEFAULT_EXTRA_SCALE;

    public float getMovieFactor(int index) {
        if (moveFactors == null) {
            return 1;
        }
        if (index < moveFactors.size()) {
            return moveFactors.get(index);
        }
        return 1;
    }

    public void addImage(String image) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(image);
    }

    public void addFactor(float factor) {
        if (moveFactors == null) {
            moveFactors = new ArrayList<>();
        }
        moveFactors.add(factor);
    }

    public boolean isInvalid() {
        return images == null || images.isEmpty();
    }

    public static ImageWallpaperMeta fromJson(String json) {
        try {
            ImageWallpaperMeta bean = new ImageWallpaperMeta();

            JSONObject object = new JSONObject(json);
            bean.moveDistance = object.optInt("distance");
            bean.extraScale = (float) object.optDouble("extraScale");

            JSONArray images = object.optJSONArray("images");
            if (images != null) {
                for (int i = 0; i < images.length(); i++) {
                    String path = images.getString(i);
                    bean.addImage(path);
                }
            }

            JSONArray factors = object.optJSONArray("factors");
            if (factors != null) {
                for (int i = 0; i < factors.length(); i++) {
                    double factor = factors.getDouble(i);
                    bean.addFactor((float) factor);
                }
            }

            return bean;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public String toJson() {
        JSONObject object = new JSONObject();
        if (images != null) {
            JSONArray images = new JSONArray();
            for (String image : this.images) {
                images.put(image);
            }
            try {
                object.put("images", images);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (moveFactors != null) {
            JSONArray factors = new JSONArray();
            for (Float factor : moveFactors) {
                try {
                    factors.put(factor.doubleValue());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            try {
                object.put("factors", factors);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            object.put("distance", moveDistance);
            object.put("extraScale", extraScale);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object.toString();
    }
}
