# VELQOR PRO — Release Notes v2.2.0 (Liquid Glass iOS)

## 1. Kết Quả Kiểm Thử (Verification Suite)
- **Kiểm thử JVM**: 90/90 tests passed (100%).
- **Kiến trúc**: 0 comment trong toàn bộ mã Java, 0 mock data, 0 thư viện ngoài.
- **Biên dịch & Đóng gói**: Thành công qua AOSP toolchain (`aapt2`, `javac`, `d8`, `apksigner`).

---

## 2. Các Thay Đổi & Nâng Cấp Cốt Lõi

### ui/SpringSolver.java (Mới)
- Bộ tích phân con 4 bước (semi-implicit Euler) theo delta-time thực.
- Dao động tắt dần có dao động nhẹ (underdamped spring, overshoot 4–6%, ổn định tại 350–450ms).
- Hàm dự đoán tab đích theo vận tốc quẹt (`computeSnapIndex`) và kháng cự kéo biên (`applyRubberBand`).

### ui/FloatingTabBar.java (Nâng Cấp Liquid Glass 100%)
- **Cấu trúc Bar**: Chiều cao 64dp, bo tròn capsule hoàn toàn, đổ bóng kép (ambient mềm + key sắc nét).
- **Viên Giọt Nước Liquid Capsule**:
  - Tích hợp cầu nối chất lỏng (metaball bridge) nối mượt hai vị trí khi di chuyển nhanh.
  - Vòm sáng specular trên đỉnh, khúc xạ phát sáng đáy (refraction rim) và viền highlight 0.75dp.
  - Biến dạng thể tích theo vận tốc tức thời: `scaleX` giãn nở khi bay, `scaleY` nén lại tương ứng.
  - Hiệu ứng thấu kính (lens effect): phóng to icon được chọn 1.14x – 1.18x với pop bounce.
- **Cử chỉ Touch & Drag Scrub**:
  - Chạm xuống viên phình to 1.12x ngay lập tức, toàn bộ bar nở nhẹ 1.02x.
  - Kéo ngang (drag scrub) dính ngón tay theo quán tính `VelocityTracker`, trượt qua tab nào tab đó phóng to và rung `CLOCK_TICK`.
  - Kháng cự kéo quá biên (rubber-band) chuẩn xác.

### ui/IosSwitchView.java
- Nút tròn (knob) phình ngang 1.2x khi nhấn chạm, trượt mượt mà và phản hồi haptic chuẩn.

### ui/BlurBackdropView.java
- Khử sạch comment, tối ưu render node trên API 31+ và fallback tự động.
