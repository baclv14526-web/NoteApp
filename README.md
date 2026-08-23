# 📒 NoteApp – Ghi Chú Android

Ứng dụng ghi chú hiện đại cho Android, viết bằng **Kotlin + MVVM + Room + Paging 3**.

---

## ✨ Tính năng

| Tính năng | Chi tiết |
|-----------|----------|
| 🎨 Tuỳ chỉnh ghi chú | Màu nền, màu chữ, ảnh nền (PNG/JPG/JPEG/WEBP) |
| 🔍 Tìm kiếm | Tìm theo tiêu đề và nội dung, real-time |
| 📁 Danh mục | Tạo/sửa/xóa, lọc ghi chú theo danh mục |
| 🏷️ Tags | Gán nhiều tag, lọc theo tag |
| 📄 Phân trang | Paging 3 – mượt dù có hàng nghìn ghi chú |
| 🔐 Bảo mật | Mã PIN 6 số (SHA-256) + vân tay (BiometricPrompt) |
| 📤 Export | Xuất toàn bộ ra `.txt` hoặc `.json` |
| 📥 Import | Nhập từ file `.txt` hoặc `.json` |
| 📌 Ghim | Ghim ghi chú quan trọng lên đầu |
| 🔄 Auto-save | Tự động lưu khi rời màn hình editor |

---

## 🏗️ Cấu trúc dự án

```
NoteApp/
├── app/src/main/java/com/noteapp/
│   ├── data/
│   │   ├── db/
│   │   │   ├── entities/        # Note, Category, Tag, NoteTagCrossRef
│   │   │   ├── dao/             # NoteDao, CategoryDao, TagDao
│   │   │   └── AppDatabase.kt
│   │   └── repository/
│   │       └── NoteRepository.kt
│   ├── ui/
│   │   ├── home/               # HomeFragment + ViewModel + NoteAdapter
│   │   ├── editor/             # EditorFragment + ViewModel
│   │   ├── security/           # PinFragment
│   │   ├── category/           # CategoryFragment + ViewModel
│   │   └── settings/           # SettingsFragment
│   └── utils/
│       ├── SecurityManager.kt  # PIN + Biometric
│       └── ExportImportUtil.kt
└── .github/workflows/
    └── android.yml             # CI/CD
```

---

## 🚀 Cài đặt và chạy

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 17
- Android SDK 34
- Min SDK: 26 (Android 8.0)

### Các bước

```bash
# 1. Clone repo
git clone https://github.com/your-username/NoteApp.git
cd NoteApp

# 2. Mở Android Studio → Open → chọn thư mục NoteApp

# 3. Sync Gradle (Android Studio tự động hỏi)

# 4. Run trên emulator hoặc thiết bị thật
```

---

## 🔧 GitHub Actions CI/CD

### Debug APK (tự động, không cần cấu hình thêm)

Mỗi lần push lên `main`/`master`/`develop`, workflow tự động:
1. Build Debug APK
2. Build Release APK (unsigned)
3. Upload cả hai làm artifact tải về được

### Release APK có chữ ký (cần cấu hình secrets)

Để tự động ký và release APK:

#### Bước 1: Tạo keystore

```bash
keytool -genkey -v \
  -keystore noteapp-release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias noteapp
```

#### Bước 2: Encode sang base64

```bash
# Linux/macOS
base64 -i noteapp-release.jks | pbcopy   # macOS copy to clipboard
base64 -i noteapp-release.jks            # Linux – copy output

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("noteapp-release.jks"))
```

#### Bước 3: Thêm Secrets vào GitHub

Vào **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Giá trị |
|-------------|---------|
| `KEYSTORE_BASE64` | Chuỗi base64 của file `.jks` |
| `KEYSTORE_PASSWORD` | Mật khẩu keystore |
| `KEY_ALIAS` | Tên alias (vd: `noteapp`) |
| `KEY_PASSWORD` | Mật khẩu key |

#### Bước 4: Thêm Variable

Vào **Settings → Secrets and variables → Actions → Variables → New**:

| Variable | Giá trị |
|----------|---------|
| `HAS_KEYSTORE` | `true` |

#### Bước 5: Tạo GitHub Release tự động

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow sẽ tự build APK đã ký và tạo GitHub Release.

---

## 🔐 Hướng dẫn tính năng bảo mật

### Tạo mã PIN
- Vào **Cài đặt → Tạo / Đổi PIN**
- Nhập 6 chữ số
- PIN được mã hóa SHA-256, lưu local

### Bảo mật ghi chú
- Trong editor, nhấn icon 🔒
- Ghi chú bị khóa → hiển thị icon 🔒 trong danh sách
- Khi mở → yêu cầu PIN hoặc vân tay

### Vân tay
- Tự động khả dụng nếu thiết bị hỗ trợ
- Hiển thị nút 👆 trong màn hình PIN

---

## 📦 Export / Import

### Export
- **Menu (⋮) → Xuất dữ liệu** hoặc **Cài đặt → Xuất**
- Chọn định dạng `.txt` hoặc `.json`
- File được chia sẻ qua ứng dụng (Drive, Gmail, v.v.)

### Import
- **Menu (⋮) → Nhập dữ liệu** hoặc **Cài đặt → Nhập**
- Chọn file `.txt` hoặc `.json` từ thiết bị
- Ghi chú và tags được tạo tự động

### Định dạng JSON
```json
{
  "version": 1,
  "notes": [
    {
      "id": 1,
      "title": "Tiêu đề",
      "content": "Nội dung...",
      "tags": ["công việc", "quan trọng"],
      "backgroundColor": -1,
      "textColor": -14540254,
      "isPinned": false,
      "isSecure": false,
      "createdAt": 1700000000000,
      "updatedAt": 1700000000000
    }
  ]
}
```

---

## 📚 Thư viện sử dụng

| Thư viện | Mục đích |
|----------|----------|
| Room 2.6.1 | Local database |
| Paging 3 | Phân trang thông minh |
| Navigation Component | Điều hướng fragment |
| ViewModel + LiveData | MVVM architecture |
| Glide 4.16 | Load ảnh nền |
| BiometricPrompt | Xác thực vân tay |
| Gson | Serialize JSON |
| Material Design 3 | UI components |
| FlexboxLayout | Layout chips tags |
| Coroutines | Async operations |
