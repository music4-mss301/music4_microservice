package vn.edu.fpt.sba.music4.services;

import org.springframework.web.multipart.MultipartFile;

public interface IImageUploadService {
    String uploadUserAvatar(Integer userId, MultipartFile file);
    String uploadArtistAvatar(Integer artistId, MultipartFile file);
    String uploadAlbumCover(Integer albumId, MultipartFile file);
}