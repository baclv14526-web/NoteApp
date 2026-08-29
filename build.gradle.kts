// Phiên bản plugin được cố định ở đây để tránh lệch version giữa local và CI.
// AGP 8.4.2 yêu cầu Gradle tối thiểu 8.6 — workflow CI cài Gradle 8.7 nên tương thích.
plugins {
    id("com.android.application") version "8.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
