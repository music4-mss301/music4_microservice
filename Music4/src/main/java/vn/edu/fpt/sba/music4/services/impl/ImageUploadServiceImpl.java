package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.AlbumRepository;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IImageUploadService;
import vn.edu.fpt.sba.music4.util.AWSUtil;

@Service
public class ImageUploadServiceImpl implements IImageUploadService {

    private final AWSUtil awsUtil;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;

    public ImageUploadServiceImpl(AWSUtil awsUtil,
                                  UserRepository userRepository,
                                  ArtistRepository artistRepository,
                                  AlbumRepository albumRepository) {
        this.awsUtil = awsUtil;
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadUserAvatar(Integer userId, MultipartFile file) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

            // Đẩy vào thư mục "avatars" trên S3
            String s3Key = awsUtil.uploadFile(file, "avatars");
            user.setImg(s3Key);
            userRepository.save(user);

            return s3Key;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi up avatar user: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadArtistAvatar(Integer artistId, MultipartFile file) {
        try {
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ với ID: " + artistId));

            String s3Key = awsUtil.uploadFile(file, "avatars");
            artist.setImg(s3Key);
            artistRepository.save(artist);

            return s3Key;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi up avatar nghệ sĩ: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAlbumCover(Integer albumId, MultipartFile file) {
        try {
            Album album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Album với ID: " + albumId));

            // Đẩy vào thư mục "albums" trên S3
            String s3Key = awsUtil.uploadFile(file, "albums");
            album.setImg(s3Key);
            albumRepository.save(album);

            return s3Key;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi up ảnh bìa album: " + e.getMessage());
        }
    }
}