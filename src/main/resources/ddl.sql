CREATE TABLE `sign_missing` (
`id`  int NOT NULL AUTO_INCREMENT ,
`player`  char(40) NULL ,
`name`  char(16) NULL ,
`lasted`  int NULL ,
`missing`  int NULL ,
`missing_time`  datetime NULL ,
PRIMARY KEY (`id`),
INDEX `idx_player` (`player`, `missing_time`)
)
;
