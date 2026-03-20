package com.taytek.basehw.domain.model

enum class SortOrder(val titleRes: Int) {
    DATE_ADDED_DESC(com.taytek.basehw.R.string.sort_newest),
    DATE_ADDED_ASC(com.taytek.basehw.R.string.sort_oldest),
    BRAND_ASC(com.taytek.basehw.R.string.sort_brand_az),
    YEAR_DESC(com.taytek.basehw.R.string.sort_year_new),
    YEAR_ASC(com.taytek.basehw.R.string.sort_year_old),
    PRICE_DESC(com.taytek.basehw.R.string.sort_price_high),
    PRICE_ASC(com.taytek.basehw.R.string.sort_price_low)
}
