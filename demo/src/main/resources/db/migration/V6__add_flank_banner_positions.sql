-- Expand banners position enum to support floating side banners (flank left and flank right)
ALTER TABLE `banners`
    MODIFY COLUMN `position` ENUM('homepage_slider','sidebar','popup','category_top','flank_left','flank_right') DEFAULT 'homepage_slider';
