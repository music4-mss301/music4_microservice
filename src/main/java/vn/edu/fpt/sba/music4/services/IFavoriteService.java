package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.FavoriteResponseDTO;

import java.util.List;

public interface IFavoriteService {

    boolean toggleFavorite(Integer trackId);
    List<FavoriteResponseDTO> getMyFavorites();
}