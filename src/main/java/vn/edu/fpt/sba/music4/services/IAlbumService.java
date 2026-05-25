package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;

public interface IAlbumService {
    List<Album> getAlbumsByArtist(Integer artistId);
    Album createAlbum(Album album);
    void deleteAlbum(Integer id);

}
