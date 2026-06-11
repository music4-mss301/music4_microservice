import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Play, Music, Layers, UploadCloud, BarChart3, User as UserIcon } from 'lucide-react';
import axiosClient from '../app/axios/axiosClient';
import { useAuthStore } from '../features/auth/useAuthStore';
import { usePlayerStore } from '../features/player/usePlayerStore';
import MusicPlayer from "../layouts/components/MusicPlayer.jsx";
import MusicImage from '../layouts/components/MusicImage';
// =====================================================================
// COMPONENT PHỤ: BĂNG CHUYỀN TỰ ĐỘNG TRƯỢT (AUTO-SCROLL CAROUSEL)
// =====================================================================
const AutoScrollCarousel = ({ title, items, renderItem, onViewAll }) => {
    const scrollRef = useRef(null);
      useEffect(() => {
        if (!items || items.length === 0) return;

        const interval = setInterval(() => {
            if (scrollRef.current) {
                const { scrollLeft, scrollWidth, clientWidth } = scrollRef.current;

                // Nếu thanh cuộn đã đi đến tận cùng bên phải -> Trượt mượt mà về lại vị trí số 0
                if (Math.ceil(scrollLeft + clientWidth) >= scrollWidth) {
                    scrollRef.current.scrollTo({ left: 0, behavior: 'smooth' });
                } else {
                    // Nếu chưa, tiếp tục cuộn sang phải một khoảng bằng độ rộng của 1 Card (~240px)
                    scrollRef.current.scrollBy({ left: 240, behavior: 'smooth' });
                }
            }
        }, 3500); // Tự động trượt sau mỗi 3.5 giây

        return () => clearInterval(interval);
    }, [items]);

    return (
        <section className="mb-12">
            <div className="flex items-center justify-between mb-5">
                <h3 className="text-2xl font-bold text-white flex items-center gap-2 tracking-tight">
                    <span className="w-1.5 h-6 bg-blue-500 rounded-full"></span>
                    {title}
                </h3>
                <button
                    onClick={onViewAll}
                    className="text-xs font-bold text-[#a7a7a7] hover:text-white uppercase tracking-widest bg-transparent border-none cursor-pointer transition-colors"
                >
                    Xem tất cả
                </button>
            </div>

            {/* Lớp bọc Carousel ẩn thanh cuộn (hide scrollbar) nhưng vẫn cho trượt */}
            <div className="relative group">
                <div
                    ref={scrollRef}
                    className="flex gap-6 overflow-x-auto scroll-smooth snap-x pb-4 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]"
                >
                    {items.length > 0 ? items.map(renderItem) : (
                        <div className="w-full text-center text-sm text-gray-500 py-8 border border-dashed border-[#282828] rounded-xl bg-[#181818]">
                            Chưa có dữ liệu hệ thống.
                        </div>
                    )}
                </div>
            </div>
        </section>
    );
};

// =====================================================================
// COMPONENT CHÍNH: TRANG CHỦ
// =====================================================================
const HomePage = () => {
    const navigate = useNavigate();
    const { username, role } = useAuthStore();

    const playTrack = usePlayerStore((state) => state.playTrack);

    // --- QUẢN LÝ TRẠNG THÁI DỮ LIỆU TỪ DATABASE ---
    const [tracks, setTracks] = useState([]);
    const [artists, setArtists] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [albums, setAlbums] = useState([])
    const [top5Tracks, setTop5Tracks]= useState([])

    // --- FETCH DATA TỪ SPRING BOOT ---
    useEffect(() => {
        const fetchHomeData = async () => {
            setIsLoading(true);
            try {
                // Gọi API lấy toàn bộ Track và Artist
                const [tracksRes, artistsRes, albumRes,top5TrackRes] =
                    await Promise.all([
                    axiosClient.get('/tracks'),
                    axiosClient.get('/artists'),
                        axiosClient.get('/albums'),
                        axiosClient.get('tracks/top5-views')
                ]);

                setTracks(tracksRes.data || []);
                setArtists(artistsRes.data || []);
                setAlbums(albumRes.data || [])
                setTop5Tracks(top5TrackRes.data || [])
            } catch (error) {
                console.error("Lỗi lấy dữ liệu trang chủ: ", error);
            } finally {
                setIsLoading(false);
            }

        };

        fetchHomeData();
    }, []);



    if (isLoading) {
        return (
            <div className="p-6 bg-[#121212] min-h-screen flex items-center justify-center font-sans">
                <div className="w-10 h-10 border-4 border-[#282828] border-t-blue-500 rounded-full animate-spin"></div>
            </div>
        );
    }

    return (
        <div className="p-6 pb-32 font-sans text-gray-100 bg-[#121212] min-h-screen selection:bg-blue-600 selection:text-white">

            {/* KHỐI BANNER CHÀO MỪNG */}
            <div className="mb-10 p-8 rounded-xl bg-[#181818] border border-[#282828] shadow-md relative overflow-hidden">
                <h2 className="text-4xl font-extrabold mb-2 text-white relative z-10 tracking-tight">
                    Chào buổi chiều, <span className="text-blue-500">{username || 'Listener'}</span>
                </h2>
                <p className="text-[#a7a7a7] relative z-10 text-sm">
                    Hệ thống gợi ý đã sẵn sàng. Khám phá những giai điệu dành riêng cho bạn hôm nay.
                </p>
            </div>


            <div className="space-y-12 animate-fadeIn">

                {/* 1. BĂNG CHUYỀN BÀI HÁT (TRACKS CAROUSEL) */}
                <AutoScrollCarousel
                    title="Khám phá Bài Hát"
                    items={tracks}
                    onViewAll={() => navigate('/tracks')} // Điều hướng sang trang xem tất cả
                    renderItem={(track) => (
                        <div
                            key={track.id}
                            className="min-w-[160px] md:min-w-[200px] flex-shrink-0 snap-start bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e] relative"
                        >
                            <div className="relative w-full h-[150px] md:h-[168px] mb-4 rounded-md overflow-hidden bg-[#282828] shadow-md flex-shrink-0">
                                <MusicImage
                                    src={track.img}
                                    type="track"
                                    alt={track.name}
                                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                />
                                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity duration-300">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            playTrack(track, tracks); // Nên truyền thêm tham số 'tracks' ngữ cảnh để kích hoạt hàng đợi chuyển bài
                                        }}
                                        className="w-11 h-11 bg-blue-600 rounded-full flex items-center justify-center text-white transform translate-y-3 group-hover:translate-y-0 transition-all duration-300 border-none cursor-pointer shadow-md"
                                    >
                                        <Play size={20} fill="currentColor" className="text-black ml-0.5" />
                                    </button>
                                </div>
                            </div>
                            <h4 className="font-bold text-white truncate text-sm mb-1">{track.name}</h4>
                            <p className="text-xs text-[#a7a7a7] truncate flex gap-1 items-center">
                                {track.artists && track.artists.length > 0 ? (
                                    track.artists.map((artist, idx) => (
                                        <span key={artist.id}>
                            <span
                                onClick={(e) => {
                                    e.stopPropagation(); // Ngăn chặn sự kiện click lan ra thẻ cha (làm phát nhạc ngoài ý muốn)
                                    navigate(`/artist/${artist.id}`); // Chuyển hướng đến trang nghệ sĩ bằng ID
                                }}
                                className="hover:text-blue-500 hover:underline cursor-pointer transition-colors text-gray-400 font-medium"
                            >
                                {artist.name}
                            </span>
                                            {/* Nếu bài hát có nhiều nghệ sĩ, thêm dấu phẩy ngăn cách ở giữa */}
                                            {idx < track.artists.length - 1 && ", "}
                        </span>
                                    ))
                                ) : (
                                    "Nghệ sĩ hệ thống"
                                )}
                            </p>
                        </div>
                    )}
                />

                {/* 2. BĂNG CHUYỀN NGHỆ SĨ (ARTISTS CAROUSEL) */}
                <AutoScrollCarousel
                    title="Nghệ Sĩ Nổi Bật"
                    items={artists}
                    onViewAll={() => navigate('/artists')} // Điều hướng sang trang xem tất cả
                    renderItem={(artist) => (
                        <div
                            key={artist.id}
                            onClick={() => navigate(`/artists/${artist.id}`)}
                            className="min-w-[160px] md:min-w-[200px] flex-shrink-0 snap-start bg-[#181818] p-5 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e] text-center"
                        >
                            {/* Khung hình tròn đặc trưng cho Nghệ sĩ */}
                            <div className="w-28 h-28 md:w-32 md:h-32 mx-auto mb-4 rounded-full overflow-hidden border border-[#282828] relative bg-[#282828] shadow-md">
                                <MusicImage
                                    src={artist.img}
                                    type="artist"
                                    alt={artist.name}
                                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 object-top" />
                                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                    <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center shadow-md transform translate-y-2 group-hover:translate-y-0 transition-all duration-300">
                                        <UserIcon size={18} className="text-white" />
                                    </div>
                                </div>
                            </div>
                            <h4 className="font-bold text-white truncate text-sm mb-1 group-hover:text-blue-400 transition-colors">{artist.name}</h4>
                            <p className="text-[11px] text-[#a7a7a7] font-medium tracking-wider uppercase">Artist</p>
                        </div>
                    )}
                />
                <AutoScrollCarousel
                    title="Album Nổi Bật"
                    items={albums}
                    onViewAll={() => navigate('/albums')}
                    renderItem={(album) => (
                        <div
                            key={album.id}
                            onClick={() => navigate(`/albums/${album.id}`)}
                            className="min-w-[160px] md:min-w-[200px] flex-shrink-0 snap-start bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e]"
                        >
                            {/* Khung hình tròn đặc trưng cho Nghệ sĩ */}
                            <div className="relative aspect-square w-full mb-4 rounded-md overflow-hidden bg-[#282828] shadow-md">
                                <MusicImage src={album.img }
                                            type={"album"}
                                            alt={album.name}
                                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                    <button className="w-11 h-11 bg-blue-600 rounded-full flex items-center justify-center shadow-md transform translate-y-3 group-hover:translate-y-0 transition-all duration-300 border-none cursor-pointer">
                                        <Play size={20} fill="currentColor" className="text-black ml-0.5" />
                                    </button>
                                </div>
                            </div>
                            <h4 className="font-bold text-white truncate text-sm mb-1 group-hover:text-blue-400 transition-colors">{album.name}</h4>
                            <p className="text-[11px] text-[#a7a7a7] font-medium tracking-wider uppercase">Album</p>
                        </div>
                    )}
                />
                <section className="mb-12">
                    <div className="flex items-center justify-between mb-5">
                        <h3 className="text-2xl font-bold text-white flex items-center gap-2 tracking-tight">
                            <span className="w-1.5 h-6 bg-blue-500 rounded-full"></span>
                            Biểu Đồ Top 5 Thịnh Hành
                        </h3>
                    </div>

                    <div className="bg-[#181818] border border-[#282828] p-6 rounded-2xl shadow-2xl">
                        {top5Tracks.length > 0 ? (
                            <div className="flex flex-col gap-2">
                                {top5Tracks.map((track, index) => {
                            // Định nghĩa màu sắc cố định theo thứ hạng
                                    let rankColor = "text-[#535353]"; // Mặc định top 4, 5
                                    if (index === 0) rankColor = "text-[#FFD700]"; // Gold
                                    if (index === 1) rankColor = "text-[#C0C0C0]"; // Silver
                                    if (index === 2) rankColor = "text-[#CD7F32]"; // Bronze

                                    return (
                                        <div
                                            key={track.id}
                                            onClick={() => playTrack(track, top5Tracks)}
                                            className="grid grid-cols-12 items-center gap-4 p-3 rounded-xl hover:bg-[#282828]/60 transition-all duration-300 group cursor-pointer"
                                        >
                                            {/* CỘT 1: THỨ HẠNG (Kích thước bằng nhau tuyệt đối) */}
                                            <div className={`col-span-1 flex items-center justify-center font-black text-xl w-10 h-10 ${rankColor}`}>
                                                {index + 1 < 10 ? `0${index + 1}` : index + 1}
                                            </div>

                                            {/* CỘT 2: HÌNH ẢNH BÀI HÁT */}
                                            <div className="col-span-1 flex items-center justify-center">
                                                <div className="w-12 h-12 rounded-md overflow-hidden bg-[#282828] relative shadow-md flex-shrink-0">
                                                    <MusicImage
                                                        src={track.img}
                                                        type={'track'}
                                                        alt={track.name}
                                                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                                    />
                                                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                                                        <Play size={14} fill="currentColor" className="text-white ml-0.5" />
                                                    </div>
                                                </div>
                                            </div>

                                            {/* CỘT 3: TÊN BÀI HÁT & NGHỆ SĨ (Chiếm không gian lớn kèm thanh đồ thị) */}
                                            <div className="col-span-7 flex flex-col justify-center min-w-0 px-2">
                                                <div className="truncate mb-1.5">
                                                    <h4 className="text-sm font-bold text-white group-hover:text-blue-400 transition-colors truncate">
                                                        {track.name}
                                                    </h4>

                                                    {/* DUYỆT MẢNG ĐỂ IN TÊN VÀ TẠO LINK ĐIỀU HƯỚNG THEO ARTIST ID */}
                                                    <p className="text-xs text-[#a7a7a7] truncate flex gap-1 items-center">
                                                        {track.artists && track.artists.length > 0 ? (
                                                            track.artists.map((artist, idx) => (
                                                                <span key={artist.id}>
                        <span
                            onClick={(e) => {
                                e.stopPropagation(); // Ngăn chặn hành vi click lan ra dòng cha gây phát nhạc ngoài ý muốn
                                navigate(`/artist/${artist.id}`); // Điều hướng người dùng sang trang cá nhân nghệ sĩ
                            }}
                            className="hover:text-blue-400 hover:underline cursor-pointer transition-colors"
                        >
                            {artist.name}
                        </span>
                                                                    {/* Nếu bài hát được thể hiện bởi nhiều nghệ sĩ, tự động thêm dấu phẩy ngăn cách */}
                                                                    {idx < track.artists.length - 1 && ", "}
                    </span>
                                                            ))
                                                        ) : (
                                                            <span>Nghệ sĩ hệ thống</span>
                                                        )}
                                                    </p>
                                                </div>
                                            </div>

                                            {/* CỘT 4: SỐ LƯỢT NGHE (Căn bên phải, hiển thị gọn gàng) */}
                                            <div className="col-span-3 text-right flex flex-col items-end justify-center">
                                <span className="text-sm font-semibold text-white font-mono bg-[#282828] px-3 py-1.5 rounded-full border border-[#3e3e3e] group-hover:border-blue-500/50 transition-colors">
                                    {track.viewCount?.toLocaleString() || 0} <span className="text-xs text-[#a7a7a7] font-sans font-normal ml-0.5">lượt nghe</span>
                                </span>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <div className="text-center text-sm text-gray-500 p-8 border border-dashed border-[#3e3e3e] rounded-xl">
                                Hệ thống đang tải dữ liệu biểu đồ...
                            </div>
                        )}
                    </div>
                </section>
            </div>

        </div>
    );
};

export default HomePage;