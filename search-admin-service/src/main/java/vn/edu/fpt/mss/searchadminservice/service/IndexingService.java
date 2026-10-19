package vn.edu.fpt.mss.searchadminservice.service;

import vn.edu.fpt.mss.searchadminservice.dto.client.AlbumDoc;
import vn.edu.fpt.mss.searchadminservice.dto.client.ArtistDoc;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackDoc;
import vn.edu.fpt.mss.searchadminservice.dto.response.ReindexStatus;

public interface IndexingService {
    void indexTrack(TrackDoc track);

    void deleteTrack(Integer trackId);

    void indexAlbum(AlbumDoc album);

    void deleteAlbum(Integer albumId);

    void indexArtist(ArtistDoc artist);

    void deleteArtist(Integer artistId);

    void renameCategory(Integer categoryId, String newName);

    void removeCategory(Integer categoryId);

    ReindexStatus startReindex();

    ReindexStatus status();

    int embedPending();
}
