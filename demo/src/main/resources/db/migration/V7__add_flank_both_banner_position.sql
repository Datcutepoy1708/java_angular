-- Expand banners position enum to support floating side banners for both flanks (flank_both)
ALTER TABLE `banners`
    MODIFY COLUMN `position` ENUM('homepage_slider','sidebar','popup','category_top','flank_left','flank_right','flank_both') DEFAULT 'homepage_slider';
