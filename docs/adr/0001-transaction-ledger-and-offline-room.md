# 0001: 採用交易流水帳（Transaction Ledger）與 Room KMP 離線架構

為支援手動記錄美股與台股持股成本，我們決定採用不可變交易流水帳（Transaction Ledger）作為資料的唯一事實來源（Single Source of Truth），並以官方 `androidx.room` (Room KMP) 實作本地離線資料庫。持倉庫存（Holding）與加權平均成本由系統透過 SQL 聚合與 Kotlin Flow 動態推導，而非由使用者手動修改餘額。這確保了完整的買賣歷史軌跡、已實現損益計算的精確度，以及未來接入即時行情 API 時的無縫擴展性。
