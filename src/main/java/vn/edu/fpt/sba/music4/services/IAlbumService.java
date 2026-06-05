package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.AlbumCreateDTO;
import vn.edu.fpt.sba.music4.dtos.response.AlbumResponseDTO;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;

public interface IAlbumService {
    List<AlbumResponseDTO> getAlbumsByArtist(Integer artistId);
    void deleteAlbum(Integer id);
    Album createAlbum(AlbumCreateDTO dto);
}
