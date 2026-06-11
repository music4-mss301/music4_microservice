import React, { useState, useEffect } from 'react';
    import { useSearchParams, useNavigate } from 'react-router-dom';
    import { Play, User as UserIcon, Music, Layers } from 'lucide-react';
    import axiosClient from '../app/axios/axiosClient';
    import { usePlayerStore } from '../features/player/usePlayerStore';
    import MusicImage from '../layouts/components/MusicImage';
    const SearchPage = () => {
        const [searchParams] = useSearchParams();
        const navigate = useNavigate();
    const playTrack = usePlayerStore((state) => state.playTrack);
    // 1. ĐỌC THAM SỐ TỪ URL (Mặc định query rỗng và type là 'all')
    const query = searchParams.get('q') || '';
    const currentType = searchParams.get('type') || 'all';

    // 2. QUẢN LÝ TRẠNG THÁI DỮ LIỆU & LOADING
    const [searchResults, setSearchResults] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    // 3. EFFECT TỰ ĐỘNG GỌI API KHI TỪ KHÓA HOẶC TAB THAY ĐỔI
    useEffect(() => {
        if (!query.trim()) {
            setSearchResults(null);
            return;
        }

        const fetchResults = async () => {
            setIsLoading(true);
            try {
                // Gọi API khớp chính xác các RequestParam của Spring Boot Controller
                const response = await axiosClient.get('/search', {
                    params: {
                        q: query,
                        type: currentType,
                        page: 0,  // Mặc định lấy trang đầu tiên
                        size: 20  // Cấu hình kích thước trang
                    }
                });
                setSearchResults(response.data);
            } catch (error) {
                console.error("Lỗi khi gọi API tìm kiếm: ", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchResults();
    }, [query, currentType]);

    useEffect(() => {
        if (searchResults) {
            console.log("Dữ liệu BE trả về:", searchResults);
        }
    }, [searchResults]);
    // 4. BÓC TÁCH MẢNG DỮ LIỆU TỪ ĐỐI TƯỢNG PAGE (.content)
    const tracks = searchResults?.tracks?.content || [];
    const artists = searchResults?.artists?.content || [];
    const albums = searchResults?.albums?.content || [];
    const playlists = searchResults?.playlists?.content || [];
    const categories = searchResults?.categories?.content || [];

    // Hàm thay đổi Tab bộ lọc bằng cách cập nhật URL
    const handleTypeChange = (newType) => {
        navigate(`/search?q=${encodeURIComponent(query)}&type=${newType}`);
    };



    // Giao diện vòng xoay khi đang tải dữ liệu
    if (isLoading) {
        return (
            <div className="p-6 bg-[#121212] min-h-screen flex items-center justify-center font-sans">
                <div className="w-10 h-10 border-4 border-[#282828] border-t-blue-500 rounded-full animate-spin"></div>
            </div>
        );
    }

    return (
        <div className="p-6 pb-32 font-sans text-gray-100 bg-[#121212] min-h-screen selection:bg-blue-600 selection:text-white">

            {/* TIÊU ĐỀ TRANG TRẠNG THÁI */}
            <div className="mb-6">
                {query ? (
                    <h2 className="text-xl text-[#a7a7a7]">
                        Kết quả tìm kiếm cho: <span className="text-white font-bold text-2xl">"{query}"</span>
                    </h2>
                ) : (
                    <h2 className="text-2xl font-bold text-white">Khám phá nội dung bạn yêu thích</h2>
                )}
            </div>

            {/* THANH CHUYỂN TABS (BỘ LỌC TYPE) */}
            <div className="flex flex-wrap gap-3 mb-10">
                {[
                    { id: 'all', label: 'Tất cả' },
                    { id: 'track', label: 'Bài hát' },
                    { id: 'artist', label: 'Nghệ sĩ' },
                    { id: 'album', label: 'Album' },
                    { id: 'playlist', label: 'Playlist' },
                    { id: 'category', label: 'Thể loại' }
                ].map((tab) => (
                    <button
                        key={tab.id}
                        onClick={() => handleTypeChange(tab.id)}
                        className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all border-none cursor-pointer ${
                            currentType === tab.id
                                ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
                                : 'bg-[#181818] text-[#a7a7a7] hover:bg-[#282828] hover:text-white'
                        }`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* KHU VỰC ĐỔ DỮ LIỆU KẾT QUẢ */}
            {/* KHU VỰC ĐỔ DỮ LIỆU KẾT QUẢ */}
            {searchResults && (tracks.length > 0 || artists.length > 0 || albums.length > 0 || playlists.length > 0 || categories.length > 0) ? (
                <div className="space-y-12 animate-fadeIn">

                    {/* KHỐI 1: HIỂN THỊ BÀI HÁT */}
                    {(currentType === 'all' || currentType === 'track') && tracks.length > 0 && (
                        <section>
                            <h3 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
                                <span className="w-1 h-5 bg-blue-500 rounded-full"></span> Bài hát
                            </h3>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                                {tracks.map((track) => (
                                    <div key={track.id} className="bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e]">
                                        <div className="relative aspect-square w-full mb-4 rounded-md overflow-hidden bg-[#282828] shadow-md">
                                            <MusicImage src={track.img}
                                            type= 'track'
                                            alt={track.name}
                                                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity duration-300">
                                                <button
                                                    onClick={(e) => {
                                                        e.stopPropagation(); // Chặn sự kiện click lan ra thẻ cha
                                                        playTrack(track, tracks); // Phát bài này và đưa toàn bộ mảng kết quả tìm kiếm 'tracks' vào hàng đợi
                                                    }}
                                                    className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white border-none cursor-pointer shadow-md"
                                                >
                                                    <Play size={18} fill="currentColor" className="text-black ml-0.5" />
                                                </button>
                                            </div>
                                        </div>
                                        <h4 className="font-bold text-white truncate text-sm mb-1">{track.name}</h4>
                                        <p className="text-xs text-[#a7a7a7] truncate">{track.viewCount?.toLocaleString() || 0} lượt nghe</p>
                                    </div>
                                ))}
                            </div>
                        </section>
                    )}

                    {/* KHỐI 2: HIỂN THỊ NGHỆ SĨ */}
                    {(currentType === 'all' || currentType === 'artist') && artists.length > 0 && (
                        <section>
                            <h3 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
                                <span className="w-1 h-5 bg-blue-500 rounded-full"></span> Nghệ sĩ
                            </h3>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                                {artists.map((artist) => (
                                    <div key={artist.id} onClick={() => navigate(`/artists/${artist.id}`)} className="bg-[#181818] p-5 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e] text-center">
                                        <div className="w-24 h-24 md:w-28 md:h-28 mx-auto mb-4 rounded-full overflow-hidden border border-[#282828] relative bg-[#282828] shadow-md">
                                            <MusicImage src={artist.img}
                                                type='artist' alt={artist.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 object-top" />
                                            <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                                <div className="w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center shadow-md">
                                                    <UserIcon size={16} className="text-white" />
                                                </div>
                                            </div>
                                        </div>
                                        <h4 className="font-bold text-white truncate text-sm mb-1 group-hover:text-blue-400 transition-colors">{artist.name}</h4>
                                        <p className="text-[11px] text-[#a7a7a7] font-medium uppercase tracking-wider">Artist</p>
                                    </div>
                                ))}
                            </div>
                        </section>
                    )}

                    {/* KHỐI 3: HIỂN THỊ ALBUM */}
                    {(currentType === 'all' || currentType === 'album') && albums.length > 0 && (
                        <section>
                            <h3 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
                                <span className="w-1 h-5 bg-blue-500 rounded-full"></span> Album
                            </h3>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                                {albums.map((album) => (
                                    <div key={album.id} onClick={() => navigate(`/albums/${album.id}`)} className="bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e]">
                                        <div className="relative aspect-square w-full mb-4 rounded-md overflow-hidden bg-[#282828] shadow-md">
                                            <MusicImage src={album.img}
                                                type='album'
                                                        alt={album.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                                <button className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center shadow-md border-none cursor-pointer">
                                                    <Play size={18} fill="currentColor" className="text-black ml-0.5" />
                                                </button>
                                            </div>
                                        </div>
                                        <h4 className="font-bold text-white truncate text-sm mb-1 group-hover:text-blue-400 transition-colors">{album.name}</h4>
                                        <p className="text-[11px] text-[#a7a7a7] font-medium uppercase tracking-wider">Album</p>
                                    </div>
                                ))}
                            </div>
                        </section>

                    )}
                    {/* KHỐI 4: HIỂN THỊ PLAYLIST */}
                    {(currentType === 'all' || currentType === 'playlist') && playlists.length > 0 && (
                        <section>
                            <h3 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
                                <span className="w-1 h-5 bg-blue-500 rounded-full"></span> Playlist
                            </h3>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                                {playlists.map((playlist) => (
                                    <div key={playlist.id} className="bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e]">
                                        {/* Vẽ giao diện playlist tương tự như Album... */}
                                        <h4 className="font-bold text-white truncate text-sm mb-1">{playlist.name}</h4>
                                        <p className="text-[11px] text-[#a7a7a7] font-medium uppercase tracking-wider">Playlist</p>
                                    </div>
                                ))}
                            </div>
                        </section>
                    )}
                    {/* KHỐI 5: HIỂN THỊ CATEGOTIES */}
                    {(currentType === 'all' || currentType === 'category') && categories.length > 0 && (
                        <section>
                            <h3 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
                                <span className="w-1 h-5 bg-blue-500 rounded-full"></span> Categories
                            </h3>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                                {categories.map((category) => (
                                    <div key={category.id} className="bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e]">
                                        {/* Vẽ giao diện playlist tương tự như Album... */}
                                        <h4 className="font-bold text-white truncate text-sm mb-1">{category.name}</h4>
                                        <p className="text-[11px] text-[#a7a7a7] font-medium uppercase tracking-wider">Category</p>
                                    </div>
                                ))}
                            </div>
                        </section>
                    )}

                </div>
            ) : (
                // TRẠNG THÁI KHÔNG TÌM THẤY DỮ LIỆU
                query && (
                    <div className="text-center py-16 border border-dashed border-[#282828] rounded-2xl bg-[#181818] max-w-xl mx-auto">
                        <p className="text-[#a7a7a7] text-base mb-2">Không tìm thấy kết quả nào phù hợp với từ khóa.</p>
                        <p className="text-xs text-gray-500">Hãy thử kiểm tra lại chính tả hoặc tìm kiếm bằng từ khóa khác nhé.</p>
                    </div>
                )
            )}
        </div>
    );
};

export default SearchPage;