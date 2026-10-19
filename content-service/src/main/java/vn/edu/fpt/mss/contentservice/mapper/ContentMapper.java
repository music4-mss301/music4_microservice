package vn.edu.fpt.mss.contentservice.mapper;

import vn.edu.fpt.mss.contentservice.dto.event.*;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.entity.*;

import java.util.List;

public final class ContentMapper {

    private ContentMapper() {
    }

    public static TrackArtistInfo toArtistInfo(TrackArtist ta) {
        return new TrackArtistInfo(ta.getArtist().getId(), ta.getArtist().getName(), ta.getRole());
    }

    public static List<TrackArtistInfo> artistsOf(Track t) {
        return t.getTrackArtists().stream().map(ContentMapper::toArtistInfo).toList();
    }

    public static TrackResponse toTrackResponse(Track t) {
        Album album = t.getAlbum();
        return new TrackResponse(t.getId(), t.getName(), t.getImg(), t.getDuration(), t.getFilePath(),
                t.getPreviewPath(), t.getViewCount(), t.getUploadDate(),
                album == null ? null : album.getId(), album == null ? null : album.getName(), artistsOf(t));
    }

    public static TrackDetailResponse toTrackDetail(Track t) {
        List<CategoryResponse> categories = t.getTrackCategories().stream()
                .map(tc -> toCategoryResponse(tc.getCategory())).toList();
        return new TrackDetailResponse(t.getId(), t.getName(), t.getImg(), t.getDuration(), t.getFilePath(),
                t.getPreviewPath(), t.getUploadDate(), t.getViewCount(),
                t.getAlbum() == null ? null : toAlbumResponse(t.getAlbum()), artistsOf(t), categories);
    }

    public static AlbumResponse toAlbumResponse(Album a) {
        return new AlbumResponse(a.getId(), a.getName(), a.getImg(), a.getUploadDate(), a.getTrackTotal(),
                a.getArtist().getId(), a.getArtist().getName());
    }

    public static ArtistResponse toArtistResponse(Artist a) {
        return new ArtistResponse(a.getId(), a.getUserId(), a.getName(), a.getImg(), a.getCover(), a.getGender(),
                a.getTrackTotal(), a.getAlbumTotal());
    }

    public static CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }

    public static TrackEventPayload toTrackEvent(Track t) {
        List<RefPayload> artists = t.getTrackArtists().stream()
                .map(ta -> new RefPayload(ta.getArtist().getId(), ta.getArtist().getName())).toList();
        List<RefPayload> categories = t.getTrackCategories().stream()
                .map(tc -> new RefPayload(tc.getCategory().getId(), tc.getCategory().getName())).toList();
        Album album = t.getAlbum();
        return new TrackEventPayload(t.getId(), t.getName(), t.getImg(), t.getDuration(), t.getFilePath(),
                t.getPreviewPath(), t.getUploadDate(), t.getViewCount(),
                album == null ? null : album.getId(), album == null ? null : album.getName(), artists, categories);
    }

    public static AlbumEventPayload toAlbumEvent(Album a) {
        return new AlbumEventPayload(a.getId(), a.getName(), a.getImg(), a.getUploadDate(), a.getTrackTotal(),
                a.getArtist().getId(), a.getArtist().getName());
    }

    public static ArtistEventPayload toArtistEvent(Artist a) {
        return new ArtistEventPayload(a.getId(), a.getUserId(), a.getName(), a.getImg(), a.getTrackTotal(),
                a.getAlbumTotal());
    }
}
