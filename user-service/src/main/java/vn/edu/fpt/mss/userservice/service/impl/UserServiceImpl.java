package vn.edu.fpt.mss.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.fpt.mss.userservice.dto.request.UpdateProfileRequest;
import vn.edu.fpt.mss.userservice.dto.response.PageResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserStatsResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserSummaryResponse;
import vn.edu.fpt.mss.userservice.entity.User;
import vn.edu.fpt.mss.userservice.enums.Role;
import vn.edu.fpt.mss.userservice.event.EventPublisher;
import vn.edu.fpt.mss.userservice.event.EventTypes;
import vn.edu.fpt.mss.userservice.exception.BusinessException;
import vn.edu.fpt.mss.userservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.userservice.mapper.UserMapper;
import vn.edu.fpt.mss.userservice.repository.UserRepository;
import vn.edu.fpt.mss.userservice.security.SecurityUtils;
import vn.edu.fpt.mss.userservice.service.RefreshTokenService;
import vn.edu.fpt.mss.userservice.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Integer id) {
        return UserMapper.toResponse(findActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String keyword, String role, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String r = StringUtils.hasText(role) ? Role.from(role).name() : null;
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        return PageResponse.of(userRepository.search(kw, r, pageable), UserMapper::toResponse);
    }

    /** Rule: chỉ chủ tài khoản hoặc ADMIN được sửa; email mới không được trùng. */
    @Override
    @Transactional
    public UserResponse updateProfile(Integer id, UpdateProfileRequest req) {
        SecurityUtils.requireOwnerOrAdmin(id);
        User user = findActive(id);
        if (StringUtils.hasText(req.email()) && !req.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(req.email(), id)) {
                throw new BusinessException("Email đã được sử dụng");
            }
            user.setEmail(req.email().trim().toLowerCase());
        }
        if (StringUtils.hasText(req.name())) {
            user.setName(req.name().trim());
        }
        if (req.img() != null) {
            user.setImg(req.img());
        }
        if (req.gender() != null) {
            user.setGender(req.gender());
        }
        eventPublisher.publish(EventTypes.USER_PROFILE_UPDATED, Map.of("userId", user.getId(),
                "name", user.getName(), "img", Objects.toString(user.getImg(), ""), "role", user.getRole()));
        return UserMapper.toResponse(user);
    }

    /** Rule: ADMIN không tự đổi role của mình và không hạ quyền admin cuối cùng. */
    @Override
    @Transactional
    public UserResponse changeRole(Integer id, String roleValue) {
        Role newRole = Role.from(roleValue);
        User user = findActive(id);
        guardNotSelf(id, "Không thể tự đổi quyền của chính mình");
        if ("ADMIN".equals(user.getRole()) && newRole != Role.ADMIN) {
            guardNotLastAdmin();
        }
        if (newRole == Role.ARTIST && user.getArtistId() == null) {
            throw new BusinessException("Tài khoản chưa có hồ sơ nghệ sĩ, hãy đăng ký tài khoản nghệ sĩ");
        }
        user.setRole(newRole.name());
        refreshTokenService.revokeAllOf(user.getUserName());
        return UserMapper.toResponse(user);
    }

    /** Rule: không khóa chính mình, không khóa admin cuối cùng; khóa thì thu hồi refresh token. */
    @Override
    @Transactional
    public UserResponse changeStatus(Integer id, boolean enabled) {
        User user = findActive(id);
        if (!enabled) {
            guardNotSelf(id, "Không thể tự khóa tài khoản của chính mình");
            if ("ADMIN".equals(user.getRole())) {
                guardNotLastAdmin();
            }
            refreshTokenService.revokeAllOf(user.getUserName());
            eventPublisher.publish(EventTypes.USER_LOCKED, Map.of("userId", id, "role", user.getRole()));
        }
        user.setEnabled(enabled);
        return UserMapper.toResponse(user);
    }

    /** Xóa mềm: giữ dòng dữ liệu để các service khác không bị "treo" id. */
    @Override
    @Transactional
    public void delete(Integer id) {
        User user = findActive(id);
        guardNotSelf(id, "Không thể tự xóa tài khoản của chính mình");
        if ("ADMIN".equals(user.getRole())) {
            guardNotLastAdmin();
        }
        user.setIsDeleted(true);
        user.setEnabled(false);
        refreshTokenService.revokeAllOf(user.getUserName());
        eventPublisher.publish(EventTypes.USER_DELETED, Map.of("userId", id, "role", user.getRole()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getSummaries(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findByIdIn(ids).stream().map(UserMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse stats() {
        return new UserStatsResponse(userRepository.countByIsDeletedFalse(),
                userRepository.countByRoleAndIsDeletedFalse("LISTENER"),
                userRepository.countByRoleAndIsDeletedFalse("ARTIST"),
                userRepository.countByRoleAndIsDeletedFalse("ADMIN"));
    }

    private User findActive(Integer id) {
        return userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + id));
    }

    private void guardNotSelf(Integer targetId, String message) {
        if (Objects.equals(SecurityUtils.currentUserIdOrNull(), targetId)) {
            throw new BusinessException(message);
        }
    }

    private void guardNotLastAdmin() {
        if (userRepository.countByRoleAndEnabledTrueAndIsDeletedFalse("ADMIN") <= 1) {
            throw new BusinessException("Hệ thống phải còn ít nhất một ADMIN đang hoạt động");
        }
    }
}
