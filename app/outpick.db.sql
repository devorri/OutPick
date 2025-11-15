BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "clothes" (
	"id"	INTEGER,
	"category"	TEXT NOT NULL,
	"image"	BLOB NOT NULL,
	"date_added"	TEXT DEFAULT CURRENT_TIMESTAMP,
	"date_bought"	TEXT,
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "outfits" (
	"id"	INTEGER,
	"outfit_name"	TEXT NOT NULL,
	"top_id"	INTEGER,
	"bottom_id"	INTEGER,
	"shoes_id"	INTEGER,
	"accessory_id"	INTEGER,
	"is_suggested"	INTEGER DEFAULT 0,
	"date_created"	TEXT DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("bottom_id") REFERENCES "clothes"("id"),
	FOREIGN KEY("shoes_id") REFERENCES "clothes"("id"),
	FOREIGN KEY("accessory_id") REFERENCES "clothes"("id"),
	FOREIGN KEY("top_id") REFERENCES "clothes"("id"),
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "outfit_history" (
	"id"	INTEGER,
	"outfit_id"	INTEGER NOT NULL,
	"date_worn"	TEXT DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("outfit_id") REFERENCES "outfits"("id"),
	PRIMARY KEY("id" AUTOINCREMENT)
);
COMMIT;
