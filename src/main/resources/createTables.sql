CREATE TABLE IF NOT EXISTS "GtfsFeed" (
  "feedID"            INTEGER PRIMARY KEY AUTOINCREMENT,
  "feedUrl"           TEXT,
  "fileLocation"      TEXT,
  "downloadTimestamp" INTEGER,
  "fileChecksum"      BLOB
);

CREATE TABLE IF NOT EXISTS "GtfsRtFeed" (
  "rtFeedID"   INTEGER PRIMARY KEY AUTOINCREMENT,
  "feedURL"    TEXT,
  "startTime"  INTEGER,
  "gtfsFeedID" INTEGER,
  FOREIGN KEY (gtfsFeedID) REFERENCES gtfsfeed (feedID)
);

CREATE TABLE IF NOT EXISTS "Error" (
  "errorID"          TEXT,
  "errorDescription" TEXT,
  PRIMARY KEY (errorID)
);

-- GTFS-RT feed error occurrences
CREATE TABLE IF NOT EXISTS "GtfsRtFeedIteration" (
  "IterationID"        INTEGER PRIMARY KEY AUTOINCREMENT,
  "IterationTimestamp" INTEGER,
  "feedProtobuf"       BLOB,
  "rtFeedID"           INTEGER,
  FOREIGN KEY (rtFeedID) REFERENCES gtfsRtFeed (rtFeedID)
);

CREATE TABLE IF NOT EXISTS "MessageLog" (
  "messageID"    INTEGER PRIMARY KEY AUTOINCREMENT,
  "iterationID"  INTEGER,
  "errorID"      TEXT,
  "errorDetails" TEXT,
  FOREIGN KEY (iterationID) REFERENCES GtfsRtFeedIteration (iterationID),
  FOREIGN KEY (errorID) REFERENCES Error (errorID)
);

CREATE TABLE IF NOT EXISTS "Occurrence" (
  "occurrenceID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "messageID"    INTEGER,
  "elementPath"  TEXT,
  "elementValue" TEXT,
  FOREIGN KEY (messageID) REFERENCES MessageLog (messageID)
);

-- GTFS feed error occurrences
CREATE TABLE IF NOT EXISTS "GtfsFeedIteration" (
  "IterationID"        INTEGER PRIMARY KEY AUTOINCREMENT,
  "IterationTimestamp" INTEGER,
  "feedID"             INTEGER,
  FOREIGN KEY (feedID) REFERENCES gtfsfeed (feedID)
);

CREATE TABLE IF NOT EXISTS "GtfsMessageLog" (
  "messageID"   INTEGER PRIMARY KEY AUTOINCREMENT,
  "iterationID" INTEGER,
  "errorID"     TEXT,
  FOREIGN KEY (iterationID) REFERENCES GtfsFeedIteration (IterationID),
  FOREIGN KEY (errorID) REFERENCES Error (errorID)
);

CREATE TABLE IF NOT EXISTS "GtfsOccurrence" (
  "occurrenceID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "messageID"    INTEGER,
  "elementPath"  TEXT,
  "elementValue" TEXT,
  FOREIGN KEY (messageID) REFERENCES GtfsMessageLog (messageID)
);

-- VIEWS --
CREATE VIEW IF NOT EXISTS errorCount AS
  SELECT
    GtfsRtFeedIteration.IterationID,
    GtfsRtFeedIteration.IterationTimestamp,
    GtfsRtFeed.rtFeedID,
    GtfsRtFeed.feedURL,
    GtfsRtFeed.gtfsFeedID,
    errorCount
  FROM GtfsRtFeedIteration
    JOIN GtfsRtFeed
      ON GtfsRtFeed.rtFeedID = GtfsRtFeedIteration.rtFeedID

    LEFT JOIN
    (SELECT
       iterationID,
       COUNT(*) AS errorCount
     FROM MessageLog
     GROUP BY iterationID) `iterationErrors`
      ON iterationErrors.iterationID = GtfsRtFeedIteration.IterationID;

CREATE VIEW IF NOT EXISTS gtfsErrorCount AS
  SELECT *
  FROM GtfsMessageLog
    JOIN GtfsOccurrence ON GtfsMessageLog.messageID = GtfsOccurrence.messageID
    JOIN Error ON Error.errorID = GtfsMessageLog.errorID;

-- CREATE VIEW IF NOT EXISTS gtfsErrorCount AS
--   SELECT
--     GtfsMessageLog.messageID,
--     iterationID,
--     GtfsMessageLog.errorID,
--     errorDescription,
--     feedUrl,
--     fileLocation,
--     downloadTimestamp,
--     errorCount
--   FROM GtfsMessageLog
--     JOIN GtfsFeed
--       ON GtfsFeed.feedID = GtfsMessageLog.iterationID
--
--     LEFT JOIN
--     (SELECT
--        messageID,
--        COUNT(*) AS errorCount
--      FROM GtfsOccurrence
--      GROUP BY messageID) `iterationErrors`
--       ON iterationErrors.messageID = GtfsMessageLog.messageID
--
--     JOIN Error
--       ON GtfsMessageLog.errorID = Error.errorID;

CREATE VIEW IF NOT EXISTS detailedError AS
  SELECT
    GtfsFeed.*,
    GtfsRtFeed.rtFeedID,
    GtfsRtFeed.feedURL,
    GtfsRtFeedIteration.IterationID,
    GtfsRtFeedIteration.IterationTimestamp,
    GtfsRtFeedIteration.feedProtobuf,
    MessageLog.messageID,
    MessageLog.errorID,
    Error.errorDescription,
    Occurrence.elementPath,
    Occurrence.elementValue
  FROM GtfsFeed
    JOIN GtfsRtFeed
      ON GtfsFeed.feedID = GtfsRtFeed.gtfsFeedID
    JOIN GtfsRtFeedIteration
      ON GtfsRtFeed.rtFeedID = GtfsRtFeedIteration.rtFeedID
    LEFT JOIN MessageLog
      ON GtfsRtFeedIteration.IterationID = MessageLog.iterationID
    LEFT JOIN Error
      ON Error.errorID = MessageLog.errorID
    LEFT JOIN Occurrence
      ON MessageLog.messageID = Occurrence.messageID;

CREATE VIEW IF NOT EXISTS messageDetails AS
  SELECT *
  FROM GtfsRtFeedIteration
    LEFT JOIN MessageLog
      ON GtfsRtFeedIteration.IterationID = MessageLog.iterationID
    LEFT JOIN Error
      ON Error.errorID = MessageLog.errorID
    LEFT JOIN Occurrence
      ON MessageLog.messageID = Occurrence.messageID;