package com.visionfit.presentation.common

import com.visionfit.domain.model.PendingStatus

data class PendingStatusCopy(val title: String, val subtitle: String, val actionLabel: String)

/** What a diary entry still in the AI pipeline says, per status. */
fun PendingStatus.copy(): PendingStatusCopy = when (this) {
    PendingStatus.ANALYZING -> PendingStatusCopy("AI đang “nếm thử”…", "Đang nhận diện từng món trong mâm", "Xem tiến trình phân tích")
    PendingStatus.WAITING_FOR_NETWORK -> PendingStatusCopy("Chờ kết nối mạng", "Ảnh sẽ được gửi phân tích khi có mạng", "Xem chi tiết")
    PendingStatus.READY_FOR_REVIEW -> PendingStatusCopy("Xong rồi! Chạm để xác nhận", "Kết quả AI đã sẵn sàng để bạn kiểm tra", "Xác nhận kết quả")
    PendingStatus.FAILED -> PendingStatusCopy("Phân tích chưa thành công", "Chạm để thử lại hoặc tự nhập món", "Thử lại")
}

/** Offline turns "analyzing" into "waiting for network": the photo cannot reach the AI. */
fun PendingStatus.effective(isOffline: Boolean): PendingStatus =
    if (isOffline && this == PendingStatus.ANALYZING) PendingStatus.WAITING_FOR_NETWORK else this
