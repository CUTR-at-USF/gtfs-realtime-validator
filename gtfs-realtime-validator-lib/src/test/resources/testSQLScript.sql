
-- Insert records into GtfsFeed table
INSERT INTO GtfsFeed -- Columns (feedId, agency, fileCheckSum, errorCount, fileLocation, feedUrl, downloadTimestamp)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    SELECT * FROM (VALUES(-1, 'America/New_York', NULL, 0, 'dummy', 'dummy', 1))
    WHERE NOT EXISTS (SELECT * FROM GtfsFeed WHERE feedId = -1);

-- Insert records into GtfsRtFeed table
INSERT INTO GtfsRtFeed -- Columns (rtFeedId, rtFeedUrl, gtfsFeedId)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    SELECT * FROM (VALUES( -1, 'dummy', -1))
    WHERE NOT EXISTS (SELECT * FROM GtfsRtFeed WHERE rtFeedId = -1);

-- Insert records into GtfsRtFeedIteration
INSERT INTO GtfsRtFeedIteration -- Columns (iterationId, feedHash, feedTimestamp, feedProtoBuf, iterationTimestamp, rtFeedId)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    SELECT * FROM (VALUES( -2, NULL, 1, NULL, 1, -1))
    WHERE NOT EXISTS (SELECT * FROM GtfsRtFeedIteration WHERE IterationId = -2);

INSERT INTO GtfsRtFeedIteration
    SELECT * FROM (VALUES( -1, NULL, 2, NULL, 2, -1))
    WHERE NOT EXISTS (SELECT * FROM GtfsRtFeedIteration WHERE IterationId = -1);

-- Insert records into MessageLog table
INSERT INTO MessageLog -- Columns (messageId, errorDetails, iterationId, errorId)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    SELECT * FROM (VALUES( -6, NULL, -2, 'W002'))
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -6);

INSERT INTO MessageLog
    SELECT * FROM (VALUES( -5, NULL, -2, 'W001'))
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -5);

INSERT INTO MessageLog
    SELECT * FROM (VALUES( -4, NULL, -2, 'E002'))
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -4);

INSERT INTO MessageLog
    SELECT * FROM (VALUES( -3, NULL, -1, 'W002'))
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -3);

INSERT INTO MessageLog
    SELECT * FROM (VALUES( -2, NULL, -1, 'W001'))
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -2);

INSERT INTO MessageLog
    SELECT * FROM (VALUES( -1, NULL, -1, 'E002'))
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -1);