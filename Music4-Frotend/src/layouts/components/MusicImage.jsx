import React from 'react';

// Cấu hình URL gốc từ S3 bucket của bạn
const S3_BASE_URL = "https://music4-v3-storage-kenz.s3.ap-southeast-1.amazonaws.com/";

const MusicImage = ({ src, type = 'track', className = '', alt = 'Music Art' }) => {

    // Hàm xử lý logic đường dẫn ảnh
    const getImageUrl = () => {
        if (!src || src === 'null') {
            return type === 'artist'
                ? "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop"
                : "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=200&auto=format&fit=crop";
        }

        if (src.startsWith('http')) {
            return src;
        }

        return `${S3_BASE_URL}${src}`;
    };

    const fallbackImage = type === 'artist'
        ? "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop"
        : "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=200&auto=format&fit=crop";

    return (
        <img
            src={getImageUrl()}
            alt={alt}
            /* 🔥 BÍ QUYẾT Ở ĐÂY:
              - aspect-square: Ép cứng tỷ lệ khung hình luôn luôn là 1:1 (Hình vuông)
              - object-cover: Tự động lấy phần giữa của ảnh để lấp đầy khung vuông, tuyệt đối chống méo hình
              - ${className}: Cho phép các page bên ngoài tùy biến thêm kích thước cụ thể (VD: w-12, w-28, w-full...)
            */
            className={`aspect-square object-cover ${className}`}
            onError={(e) => {
                e.target.onerror = null;
                e.target.src = fallbackImage;
            }}
        />
    );
};

export default MusicImage;