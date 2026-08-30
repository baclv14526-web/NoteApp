# NoteApp (Kotlin + Jetpack Compose)

Ứng dụng ghi chú Android viết bằng Kotlin, dùng Jetpack Compose + Room + Paging3.

## Tính năng
- **Màu nền / màu chữ** cho từng ghi chú, chọn từ bảng màu có sẵn.
- **Ảnh nền tuỳ chỉnh** (.png/.jpg/.jpeg) thay thế màu nền.
- **Giao diện Material 3**, hỗ trợ Dynamic Color (Android 12+).
- **Tìm kiếm** theo tên, nội dung và tag cùng lúc, realtime.
- **Category**: lọc bằng chip trên màn hình danh sách, quản lý (thêm/xoá) ở màn hình riêng. Mỗi chip hiện kèm số lượng ghi chú, ví dụ "Tất cả (10)", "Công việc (5)" — cập nhật tự động khi thêm/xoá/đổi category của ghi chú.
- **Tag**: nhập tự do, phân tách bởi dấu phẩy.
- **Phân trang thông minh (Paging3 + Room)**: khi > 100 ghi chú, danh sách chỉ tải từng trang 20 item.
- **Export/Import**: xuất toàn bộ ghi chú ra `.txt` (dễ đọc), `.pdf` (để in/lưu trữ), hoặc `.json` (backup đầy đủ) và chia sẻ qua Intent (Zalo, Gmail, Drive...). Import lại từ file `.json` (đầy đủ dữ liệu) hoặc `.txt` (đã xuất từ chính app, hoặc file .txt tự viết — sẽ được nhập thành 1 ghi chú). File `.pdf` không hỗ trợ import lại.
- **Thùng rác**: xoá ghi chú không xoá thẳng mà chuyển vào thùng rác, có thể khôi phục hoặc xoá vĩnh viễn. Tự động dọn sạch các ghi chú đã ở trong thùng rác quá 30 ngày mỗi khi mở app.
- **Sắp xếp**: 6 kiểu — mới/cũ theo ngày sửa, mới/cũ theo ngày tạo, tên A→Z / Z→A. Ghi chú ghim luôn ưu tiên lên đầu bất kể sort theo kiểu gì.
- **Nhắc nhở (Reminder)**: đặt ngày + giờ cho từng ghi chú, dùng `AlarmManager` để bắn thông báo đúng thời điểm (kể cả khi máy đang ở chế độ Doze). Tap vào thông báo mở thẳng ghi chú đó. Tự động đặt lại toàn bộ nhắc nhở sau khi khởi động lại máy (vì `AlarmManager` bị hệ thống huỷ mỗi lần reboot).
- **App Widget (Home Screen)**: hiển thị danh sách ghi chú đã ghim ngay trên màn hình chính điện thoại, dùng Glance (Compose cho App Widget). Tap vào 1 ghi chú trong widget mở thẳng app tới ghi chú đó. Widget tự làm mới mỗi khi ghim/bỏ ghim/sửa/xoá ghi chú.
- **Ghi chú bí mật (khoá PIN 6 số)**: khoá ghi chú riêng tư bằng mật khẩu 6 số dùng chung cho cả app. Ghi chú đã khoá ẩn hoàn toàn title/content ở danh sách chính, thùng rác, không lộ qua tìm kiếm/gợi ý tag, tự động bị loại khỏi Widget và file export. Có câu hỏi bảo mật để đặt lại mật khẩu nếu quên. PIN được lưu dạng hash (SHA-256 + salt) trong `EncryptedSharedPreferences`, mã hoá bằng Android Keystore — không lưu plaintext.
- **Thông tin ghi chú**: mỗi ghi chú hiển thị ngày giờ tạo, ngày giờ sửa lần cuối, và số lần đã sửa (tăng tự động mỗi khi lưu lại ghi chú đã tồn tại).

## Mở project
1. Mở thư mục `NoteApp` bằng Android Studio (Koala trở lên).
2. Đợi Gradle sync (JDK 17, compileSdk 34, minSdk 24, AGP 8.4.2, Gradle 8.7+).
3. Run trên emulator hoặc thiết bị thật.

> **Lưu ý:** project này KHÔNG có sẵn file `gradlew`. Android Studio sẽ tự tạo Gradle Wrapper phù hợp khi bạn mở project lần đầu (hoặc bạn có thể chạy `gradle wrapper --gradle-version=8.7` nếu muốn build bằng `./gradlew` ở local). Việc không commit sẵn `gradlew` giúp tránh tình trạng version bị lệch giữa máy local và CI.

## CI/CD — GitHub Actions
File `.github/workflows/android.yml`:
- **Không dùng `./gradlew`** trong repo. Thay vào đó dùng action chính thức `gradle/actions/setup-gradle@v4` để cài **Gradle 8.7** thẳng trên runner, sau đó gọi lệnh `gradle` trực tiếp. Cách này loại bỏ hoàn toàn nguyên nhân phổ biến nhất gây lỗi `Minimum supported Gradle version is 8.6` (do file `gradle-wrapper.properties` cũ/bị lệch version).
- Dùng `android-actions/setup-android@v3` để có `$ANDROID_HOME` và `apksigner`.
- Job `build`: tạo keystore CI, build cả debug và release APK, ký tự động, verify bằng `apksigner`, upload artifact.
- Job `release-prod`: chỉ chạy khi push tag `vX.Y.Z` và đã cấu hình secrets thật (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) + biến `vars.HAS_KEYSTORE=true`.

### Cách lấy APK sau khi CI chạy
1. Tab **Actions** → chọn run mới nhất.
2. Kéo xuống **Artifacts** → tải `NoteApp-release-<số>`.
3. Giải nén → cài `app-release.apk` lên điện thoại (cần bật "Nguồn không xác định").

## Nếu build vẫn lỗi
1. Kiểm tra repo **không còn** file `gradlew`, `gradlew.bat`, thư mục `gradle/wrapper/` sót lại từ lần tạo project cũ — nếu có, xoá đi (workflow này không cần chúng).
2. Đảm bảo bạn đã copy đúng **toàn bộ** thư mục `.github/workflows/android.yml` (không chỉnh sửa tay gây sai cú pháp YAML — thụt lề sai 1 dấu cách cũng khiến job fail).
3. Xem log lỗi đầy đủ (đoạn từ `FAILURE:` trở xuống, hoặc dòng bắt đầu bằng `e:` nếu là lỗi Kotlin compile) trong tab Actions để xác định bước nào fail.

## Lưu ý về database
Schema Room hiện ở **version 5**:
- v1 → v2: thêm tính năng Thùng rác (cột `isDeleted`, `deletedAt`).
- v2 → v3: thêm tính năng Nhắc nhở (cột `reminderAt`).
- v3 → v4: thêm tính năng Ghi chú bí mật (cột `isLocked`).
- v4 → v5: thêm tính năng đếm số lần sửa (cột `editCount`).

Cả bốn bước đều có `Migration` đầy đủ trong `AppDatabase.kt` (không dùng `fallbackToDestructiveMigration`), nên **dữ liệu ghi chú cũ của người dùng không bị mất** khi cập nhật app qua các phiên bản — không cần gỡ cài đặt lại.

## Lưu ý về quyền (permissions) cho tính năng Nhắc nhở
- **POST_NOTIFICATIONS** (Android 13+): app tự xin quyền này ngay khi mở lần đầu. Nếu người dùng từ chối, nhắc nhở vẫn được lưu vào ghi chú nhưng sẽ không hiển thị thông báo — cần vào Cài đặt hệ thống bật lại thủ công.
- **SCHEDULE_EXACT_ALARM** (Android 12+): một số thiết bị/OEM yêu cầu người dùng cấp quyền này thủ công trong Cài đặt → Ứng dụng → NoteApp → Báo thức và lời nhắc. Nếu quyền bị tắt, app sẽ hiện Toast báo lỗi khi cố đặt nhắc nhở thay vì âm thầm bỏ qua.

## Cách thêm Widget vào Home Screen
1. Cài app lên máy như bình thường.
2. Nhấn giữ vào khoảng trống trên màn hình chính (Home Screen) → chọn **Widget**.
3. Tìm **NoteApp** trong danh sách → kéo thả widget "Ghi chú ghim" ra màn hình.
4. Widget hiển thị các ghi chú đã ghim (bấm icon ghim khi sửa ghi chú trong app để thêm vào widget). Nếu chưa ghim ghi chú nào, widget sẽ hiện hướng dẫn thay vì để trống.

## Cách dùng Ghi chú bí mật (khoá PIN)
1. Vào màn hình chính, bấm icon 🛡️ (Bảo mật) trên thanh trên cùng → **Đặt mật khẩu** → nhập PIN 6 số, nhập lại để xác nhận, chọn 1 câu hỏi bảo mật và điền câu trả lời.
2. Mở 1 ghi chú bất kỳ → bấm icon ổ khoá trên thanh trên cùng để khoá lại → bấm **Lưu**.
3. Ghi chú đã khoá sẽ hiện "Ghi chú bí mật" ở danh sách chính thay vì nội dung thật. Bấm vào để mở sẽ được yêu cầu nhập PIN.
4. Nếu quên PIN: ở màn nhập PIN, bấm **Quên mã PIN?** → trả lời đúng câu hỏi bảo mật đã đặt → đặt PIN mới.
5. Ghi chú bí mật **không** xuất hiện trên Widget, **không** khớp khi tìm kiếm theo nội dung, và **không** được đưa vào file export (.txt/.pdf/.json) — đảm bảo không rò rỉ ra ngoài phạm vi bảo vệ của PIN.
