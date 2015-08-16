CREATE TABLE IF NOT EXISTS "GtfsFeed" (
  "feedID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "feedUrl" TEXT,
  "fileLocation" TEXT,
  "downloadTimestamp" INTEGER
);

CREATE TABLE IF NOT EXISTS "GtfsRtFeed" (
  "rtFeedID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "feedURL" TEXT,
  "startTime" INTEGER,
  "gtfsFeedID" INTEGER,
  FOREIGN KEY (gtfsFeedID) REFERENCES gtfsfeed(feedID)
);

CREATE TABLE IF NOT EXISTS "GtfsRtFeedIteration" (
  "IterationID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "IterationTimestamp" INTEGER,
  "feedProtobuf" BLOB,
  "rtFeedID" INTEGER,
  FOREIGN KEY (rtFeedID) REFERENCES gtfsRtFeed(rtFeedID)
);

CREATE TABLE IF NOT EXISTS "Error" (
  "errorID" TEXT,
  "errorDescription" TEXT,
  PRIMARY KEY (errorID)
);

CREATE TABLE IF NOT EXISTS "MessageLog" (
  "messageID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "itterationID" INTEGER,
  "errorID" TEXT,
  "errorDetails" TEXT,
  FOREIGN KEY (itterationID) REFERENCES GtfsRtFeedIteration(itterationID),
  FOREIGN KEY (errorID) REFERENCES Error(errorID)
);

CREATE TABLE IF NOT EXISTS "Occurrence" (
  "occurrenceID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "messageID" INTEGER,
  "elementPath" TEXT,
  "elementValue" TEXT,
  FOREIGN KEY (messageID) REFERENCES MessageLog(messageID)
);

CREATE TABLE IF NOT EXISTS "GtfsMessageLog" (
  "messageID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "errorID" TEXT,
  FOREIGN KEY (errorID) REFERENCES Error(errorID)
);

CREATE TABLE IF NOT EXISTS "GtfsOccurrence" (
  "occurrenceID" INTEGER PRIMARY KEY AUTOINCREMENT,
  "messageID" INTEGER,
  "elementPath" TEXT,
  "elementValue" TEXT,
  FOREIGN KEY (messageID) REFERENCES MessageLog(messageID)
);

CREATE VIEW IF NOT EXISTS errorCount AS
  SELECT rtFeedID, feedURL, gtfsFeedID, errorItteration.* FROM GtfsRtFeed
    JOIN
    (SELECT IterationID, IterationTimestamp, Message.errorCount FROM GtfsRtFeedIteration
      left JOIN
      (SELECT COUNT(errorID) AS "errorCount", itterationID
       FROM MessageLog)`Message`
        ON GtfsRtFeedIteration.IterationID = Message.itterationID)`errorItteration`;

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
      ON GtfsRtFeedIteration.IterationID = MessageLog.itterationID
    LEFT JOIN Error
      ON Error.errorID = MessageLog.errorID
    LEFT JOIN Occurrence
      ON MessageLog.messageID = Occurrence.messageID;

CREATE VIEW IF NOT EXISTS messageDetails AS
  SELECT *
  FROM GtfsRtFeedIteration
    LEFT JOIN MessageLog
      ON GtfsRtFeedIteration.IterationID = MessageLog.itterationID
    LEFT JOIN Error
      ON Error.errorID = MessageLog.errorID
    LEFT JOIN Occurrence
      ON MessageLog.messageID = Occurrence.messageID;