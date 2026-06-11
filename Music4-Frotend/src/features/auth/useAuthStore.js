import { create } from 'zustand';

export const useAuthStore = create((set) => ({
    id: localStorage.getItem('userId') || null,
    username: localStorage.getItem('username') || null,
    img: localStorage.getItem('userImg') || null, // 🟢 Bổ sung lưu ảnh
    role: localStorage.getItem('role') || 'listener',
    isAuthenticated: !!localStorage.getItem('accessToken'),
    isLoading: false,
    error: null,

    // 🟢 Hứng thêm id và img từ file LoginPage truyền sang
    loginSuccess: (accessToken, refreshToken, id, username, img, role) => {
        let finalRole = role.replace('ROLE_', '').toLowerCase();
        if (finalRole === 'user') finalRole = 'listener';

        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('userId', id);
        localStorage.setItem('username', username);
        if (img && img !== 'null') localStorage.setItem('userImg', img);

        set({
            id: id,
            username: username,
            img: img && img !== 'null' ? img : null,
            role: finalRole,
            isAuthenticated: true,
            error: null
        });
    },

    logout: () => {
        localStorage.clear();
        set({ id: null, username: null, img: null, role: 'listener', isAuthenticated: false, error: null });
    },

    setAuthError: (errorMsg) => set({ error: errorMsg }),
    setLoading: (isLoading) => set({ isLoading }),
}));