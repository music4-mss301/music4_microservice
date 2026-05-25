package vn.edu.fpt.sba.music4.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import vn.edu.fpt.sba.music4.entities.*;

import java.util.List;


public record SearchResultResponseDTO (Page<Track> tracks,
                                       Page<Album> albums,
                                       Page<Artist> artists,
                                       Page<Playlist> playlists,
                                       Page<Category> categories){
}
