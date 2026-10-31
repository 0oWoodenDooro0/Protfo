# Portfo Domain Glossary

離線優先之 Kotlin Multiplatform 跨市場股票投資組合記帳系統。

## Language

**Transaction (交易紀錄)**:
不可變的單筆證券交易事實，包含買賣別 (Buy/Sell)、標的識別碼、成交價格、成交股數、手續費、稅金與交易時間。
_Avoid_: Order, Trade, Record, Operation

**Holding (持倉庫存)**:
由特定標的的所有有效歷史交易紀錄，依照加權平均成本法即時匯總出的當前持有狀態（包含現有股數與平均持股成本）。
_Avoid_: Position, StockInventory, Balance

**Cost Basis (成本基礎)**:
取得當前持股所累積支付的總資金（包含買入成交價款與手續費等交易成本）。
_Avoid_: InvestedAmount, Capital

**Realized PnL (已實現損益)**:
賣出股票時，賣出淨所得扣除該批股份對應之加權平均成本後的實際盈虧金額。
_Avoid_: Profit, GainLoss, NetIncome

**Fractional Shares (碎股)**:
美股市場允許之非整數股數（以小數形式表示，如 0.25 股），相對於台股之整數股或零股單位。
_Avoid_: DecimalShares, OddLot
