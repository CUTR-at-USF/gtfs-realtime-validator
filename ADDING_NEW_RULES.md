# Adding new rules

We will want to add new rules to this validator as the [GTFS Realtime spec](https://github.com/google/transit/tree/master/gtfs-realtime) and the surrounding applications and tools change.  This page outlines the process of adding new rules to this tool.

### 0. Prepare for implementation 

* Check the list of [currently implemented rules](RULES.md) to make sure the rule doesn't already exist.
* Check the list of [planned future rules](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22) to see if an issue already exists for the proposed rule.
    * If no existing issue exists, open a new issue with the ["new rule" label](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22).
* Discuss the rule with the community via the Github issue and come to a general consensus on the exact logic, and if it should be an `ERROR` or `WARNING`.  Generally, errors are behavior that directly violate the GTFS Realtime specification.  Warnings are behavior that is not advised (e.g., against best practices) but not explicitly forbidden in the GTSF Realtime specification.
* Implement new rule using the process below

For the below example, let's look at implementing a new rule that will make sure that each `vehicle.id` in a VehiclePositions feed is unique.  If there are two VehiclePosition entities in a feed with the same `vehicle.id`, then the validator should log an error.

If you want to take a look at a complete set of changes that implement this new rule before diving into the instructions, see [this commit on Github](https://github.com/CUTR-at-USF/gtfs-realtime-validator/commit/121173f5167cc0d460c3eb50b3582265470671c4).

### 1. Add the rule to `ValidationRules.java`

Rules are declared in the [`ValidationRules.java` class](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/java/edu/usf/cutr/gtfsrtvalidator/validation/ValidationRules.java).

Add the new rule there, for example:

~~~
public static final ValidationRule E052 = new ValidationRule("E052", "ERROR", "vehicle.id is not unique",
        "Each vehicle should have a unique ID",
        "which is used by more than one vehicle in the feed");
~~~

Errors should be prefixed with `W`, while errors are prefixed by `E`.

The 3rd line `is used by more than one vehicle in the feed` is the last part of a sentence (i.e., suffix) that will be logged when an error is found in the feed.

For the `vehicle.id` being unique, our final error message should look like:

`vehicle.id 5 is used by more than one vehicle in the feed`

So, the `vehicle.id 5` will change for each time the message is logged depending on the actual `vehicle.id`, but the suffix `is used by more than one vehicle in the feed` will always be the same.  So, we save that string here once to save space later when we are logging messages.

### 2. Add the rule to [`RULES.md`](RULES.md)

Add the rule to the error or warnings table at the top of [`RULES.md`](RULES.md):

~~~
| [E052](#E052) | `vehicle.id` is not unique
~~~

...and add a definition of that rule at the bottom of the errors or warnings section:

~~~
<a name="E052"/>

### E052 - `vehicle.id` is not unique

Each vehicle should have a unique ID.

From [VehiclePosition.VehicleDescriptor](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicledescriptor) for `vehicle.id`:

>Internal system identification of the vehicle. Should be unique per vehicle, and is used for tracking the vehicle as it proceeds through the system. This id should not be made visible to the end-user; for that purpose use the label field

#### References:
* [`vehicle.id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicledescriptor)
~~~

If there are common mistakes and possible solutions to those mistakes related to this rule, we can add that information to this section too.  When the user clicks on the error code in the validator web interface, they are directed to this section of the rules page so they can find out more information about the rule.  So, any information that might help an agency or AVL vendor fix the problem should be included here. 

### 3. Determine in which `*Validator.java` class the new rule should be implemented

All classes that implement rules should use a name that fits the `*Validator.java` format and must implement the [`FeedEntityValidator` interface](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/src/main/java/edu/usf/cutr/gtfsrtvalidator/lib/validation/interfaces/FeedEntityValidator.java).  For efficiency of implementation, multiple rules related to similar fields can be implemented in the same `*Validator.java` class (e.g., to avoid iterating through all messages for each rule).

Here are the currently implemented `*Validator.java` classes (all defined in [`gtfs-realtime-validator-lib`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/tree/master/gtfs-realtime-validator-lib) module in the package `edu.usf.cutr.gtfsrtvalidator.lib.validation.rules`):
*  `CrossFeedDescriptorValidator` - Examines multiple GTFS-rt feeds (e.g., comparing TripUpdates to VehiclePositions) to identify potential discrepancies between them (e.g., `E047 - VehiclePosition and TripUpdate ID pairing mismatch`).
*  `FrequencyTypeOneValidator` - Examines frequency-based type 1 trips - trips defined in GTFS frequencies.txt with `exact_times = 1`
*  `FrequencyTypeZeroValidator` - Examines frequency-based type 0 trips - trips defined in GTFS frequencies.txt with `exact_times = 0`
*  `HeaderValidator` - Examines the GTFS Realtime header
*  `StopTimeUpdateValidator` - Examines `stop_time_updates` in trips
*  `StopValidator` - Examines `stops`
*  `TimestampValidator` -  Examines entity `timestamps`
*  `TripDescriptorValidator` - Examines attributes for a trip (e.g., `trip_id`, `route_id`) contained in the TripDescriptor
*  `VehicleValidator` - Examines attributes for vehicles in [VehiclePosition](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicleposition) and child entities (e.g., [VehicleDescriptor](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicledescriptor), [Position](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position))

If the new rule doesn't fit into the scope of the above classes, you may need to implement a new `*Validator.java` class.  In this example, determining if each `vehicle.id` is unique falls under the `VehicleValidator` class, so we'll implement this rule there.

If you create a new `*Valdiator.java` class (e.g., `MyValidator.java`), you'll need to add it to the Collections that contain instances of all the `*Validator.java` classes at runtime.

Currently, this is in [`BatchProcessor.processFeeds()`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/src/main/java/edu/usf/cutr/gtfsrtvalidator/lib/batch/BatchProcessor.java):

~~~
// Initialize validation rules
synchronized (mValidationRules) {
	if (mValidationRules.isEmpty()) {
        ...
		mValidationRules.add(new HeaderValidator());
		mValidationRules.add(new MyValidator());  // <--- Add this
	}
}
~~~

...and the constructor for [`BackgroundTask`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-webapp/src/main/java/edu/usf/cutr/gtfsrtvalidator/background/BackgroundTask.java):

~~~
public BackgroundTask(GtfsRtFeedModel gtfsRtFeed) {
    ...
	// Initialize validation rules
	synchronized (mValidationRules) {
		if (mValidationRules.isEmpty()) {
            ...
			mValidationRules.add(new HeaderValidator());
		    mValidationRules.add(new MyValidator());  // <--- Add this
		}
	}
}
~~~


### 4. Add a comment at the top of the `*Validator.java` class for the new rule

To keep easy track of what rules are implemented in which `*Validator` class, add the new rule in the comment block at the top:

~~~
/**
 * E026 - Invalid vehicle position
 * E027 - Invalid vehicle bearing
 * E028 - Vehicle position outside agency coverage area
 * E029 - Vehicle position outside trip shape buffer
 * W002 - vehicle_id not populated
 * W004 - vehicle speed is unrealistic
 * E052 - vehicle.id is not unique // <--- Add this
 */

public class VehicleValidator implements FeedEntityValidator {
~~~

### 5. Add the validation logic for the new rule

This exact process will differ for each rule, but first let's cover some of the basics that are the same across any rule implementation in the `*Validator.validate()` method.

First, at the beginning of the `*Validator.validate()` method you'll need to declare a list to hold occurrences of an error:
 
 ~~~
     public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
         List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
         List<OccurrenceModel> e026List = new ArrayList<>();
         ...
         List<OccurrenceModel> e052List = new ArrayList<>(); // <--- Add this
~~~

And, at the end of the `*Validator.validate()` method you'll need to add this list of E052 occurrences to the list of all errors/warnings that are returned by this `*Validator`:

~~~
        ...
        if (!w004List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W004), w004List));
        }
        if (!e052List.isEmpty()) {  // Add this IF statement to save any logged occurrences of the new E052 rule
            errors.add(new ErrorListHelperModel(new MessageLogModel(E052), e052List));
        }
        return errors;        
~~~

Now that the boilerplate code is out of the way, we need to implement the actual logic that checks to make sure that all `vehicle.ids` are unique.

To do this, we need to loop through all entities in the feed and keep track of all vehicle IDs we've seen so far, and for each ID check it against the list of IDs we've already seen so we know if it's a duplicate.

This block of code is what loops through all entities in a feed, so generally speaking here's what we need to implement:

~~~
        for (GtfsRealtime.FeedEntity entity : entityList) {
            // TODO - For this `entity`, if it's a VehiclePosition determine if we've already seen the VehiclePosition.VehicleDescriptor.id, and if so log an E052 error
        }  
~~~

To check for duplicates efficiently, we'll use a [`HashSet`](https://docs.oracle.com/javase/9/docs/api/java/util/HashSet.html) to store the `vehicle.ids`:

~~~
        HashSet<String> vehicleIds = new HashSet<>(entityList.size()); // < --- Add this
        
        for (GtfsRealtime.FeedEntity entity : entityList) {
            // TODO - For this `entity`, if it's a VehiclePosition determine if we've already seen the VehiclePosition.VehicleDescriptor.id, and if so log an E052 error
        }  
~~~

Then, we need to check if the current `entity` is a VehiclePosition and if so, get a reference to it:

~~~
        HashSet<String> vehicleIds = new HashSet<>(entityList.size());
        
        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasVehicle()) { // < --- Add this and below line
                GtfsRealtime.VehiclePosition v = entity.getVehicle();
                // TODO - Determine if we've already seen the VehiclePosition.VehicleDescriptor.id, and if so log an E052 error
            } 
        }  
~~~

Now, we need to make sure the vehicle ID isn't an empty string - we don't want to call two occurrences of an empty vehicle ID field an error:

~~~
        HashSet<String> vehicleIds = new HashSet<>(entityList.size());
        
        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition v = entity.getVehicle();
                if (!StringUtils.isEmpty(v.getVehicle().getId())) {  // < --- Add this
                    // TODO - Determine if we've already seen the VehiclePosition.VehicleDescriptor.id, and if so log an E052 error
                }
            } 
        }  
~~~

Next, we need to look in the `HashSet` to determine if we've already seen this vehicle ID previously, and if so, log an E052 error.  If not, then add this vehicle ID to the `HashSet`.

~~~
        HashSet<String> vehicleIds = new HashSet<>(entityList.size());
        
        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition v = entity.getVehicle();
                if (!StringUtils.isEmpty(v.getVehicle().getId())) {
                    if (vehicleIds.contains(v.getVehicle().getId())) {  // < --- Add this IF/ELSE block
                        // E052 - vehicle.id is not unique
                        RuleUtils.addOccurrence(E052, "entity ID " + entity.getId() + " has vehicle.id " + v.getVehicle().getId(), e052List, _log);
                    } else {
                        vehicleIds.add(v.getVehicle().getId());
                    }
                }
            } 
        }  
~~~

To better understand that last step, let's break down the line:

`RuleUtils.addOccurrence(E052, "entity ID " + entity.getId() + " has vehicle.id " + v.getVehicle().getId(), e052List, _log);`

* `E052` - The rule we're logging the error or warning for
* `"entity ID " + entity.getId() + " has vehicle.id " + v.getVehicle().getId()` - This is the first part of a sentence (i.e., prefix) that will be logged when an error is found in the feed.  When combined with the suffix that we added earlier in the first step, the detailed output to the log for an error will look like `entity ID 1234 has vehicle.id 9876 which is used by more than one vehicle in the feed`.
* `e052List` - The list of E052 errors to which this new ocurrence will be added
* `_log` - The log to which system output will be printed for testing

That's it for the rule itself!  If you run this code it will now log an error for every vehicle with an ID that's not unique.

### 6. Add a unit test for the new rule

Like any software project, we add [unit tests](https://en.wikipedia.org/wiki/Unit_testing) for all new rules to make sure that as the application continues to grow we don't accidentally break anything.

Because we added the rule in `VehicleValidator`, we'll add the unit test for this rule in [`VehicleValidatorTest`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/src/test/java/edu/usf/cutr/gtfsrtvalidator/lib/test/rules/VehicleValidatorTest.java).

(If you created a new `MyValidator.java` class earlier, you'll want to create a new `MyValidatorTest.java` class in the [`test/rules`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/tree/master/gtfs-realtime-validator-lib/src/test/java/edu/usf/cutr/gtfsrtvalidator/lib/test/rules) directory, and have it extend [`FeedMessageTest`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/src/test/java/edu/usf/cutr/gtfsrtvalidator/lib/test/FeedMessageTest.java))

Because our test classes extend [`FeedMessageTest`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/src/test/java/edu/usf/cutr/gtfsrtvalidator/lib/test/FeedMessageTest.java), a lot of the setup of loading test GTFS data and initializing GTFS Realtime data structures is already taken care of.  We'll only cover the details that you absolutely need to know for writing a new rule. 

First, we will create a new method annotated with `@Test` in [`VehicleValidatorTest`](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/gtfs-realtime-validator-lib/src/test/java/edu/usf/cutr/gtfsrtvalidator/lib/test/rules/VehicleValidatorTest.java):

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
    
    }
    
Then, we'll add the validator class we're testing:

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        VehicleValidator vehicleValidator = new VehicleValidator();  // This contains the code for the rule we just wrote
        
    }
    
Next, we need to add a data structure to hold the expected results from the unit test.  In our case, it's the number of occurrences of each error or warning, in the form of a map of the `ValidationRule` to the number of expected errors or warnings (an `Integer`):

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        VehicleValidator vehicleValidator = new VehicleValidator();
        Map<ValidationRule, Integer> expected = new HashMap<>();  // This will contain the expected output of the validator
        
    }
    
Now we can create the GTFS Realtime VehiclePosition messages that we're going to test our rule against.  First, let's just add a single message with the vehicle ID of `1`:

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        ...

        // The below code declares a VehicleDescription with the "id" of "1"
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilder.setId("1");

        // The below code adds the above VehicleDescriptor to a VehiclePosition entity, and builds a new GTFS Realtime message that contains that entity
        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.setEntity(0, feedEntityBuilder.build());

    }
    
We now have a GTFS Realtime feed with a single `VehiclePosition` message. Note that the `vehiclePositionBuilder` we're adding the `VehicleDescriptor` has already been declared and initialized in the class we're extending, which is `FeedMessageTest`.  This cuts down on the boilerplace code in each unit test.

Now let's run our test data through the `vehicleValidator` to actually validate it using the rule we just wrote:

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        ...

        // Execute all rules in the VehicleValidator class, including E052 that we just wrote and store results in "results"
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        
    }

We again use several fields from the `FeedMessageTest` super class:
    * `results` - Used to store a list of errors and warnings that were detected
    * `bullRunnerGtfs` - GTFS data against which the rules are executed.  We support several different example GTFS datasets used to test rules under different conditions (e.g., frequency-based vs. schedule based, no shape data).  For the purpose of determining if the vehicle ID is unique, the GTFS data doesn't matter, so we can choose any GTFS dataset here.
    * `bullRunnerGtfsMetadata` - The metadata calculated for the above GTFS data.  The important part for this parameter is that this metadata is always paired with the correct GTFS data (i.e., the metadata was created using this GTFS dataset).
    
We also need to pass in the "current time" for when this data was captured, so certain rules that measure message age can be calculated.  For our purposes here the current time doesn't matter, so we just use the minimum valid POSIX time (`MIN_POSIX_TIME`).

We pass in the built testing message as `feedMessageBuilder.build()`.

We can leave the last two parameters, `previousFeedMessage` and `combinedFeedMessage` as null for this test.  `previousFeedMessage` would be the message received just prior to the current message being evaluated - this allows us to execute rules that look at the interval of time between updates, whether or not the message contents actually change, etc.  The `combinedFeedMessage`  would include entities from all GTFS-rt feeds being monitored simultaneously for the same GTFS dataset (e.g., VehiclePosition and TripUpdate).  If only one GTFS-rt feed is being monitored for the GTFS dataset, then this is null.

Now that we have the actual results of validation, we need to initialize the expected results in `expected`.  In this initial case, we only have a single vehicle, so rule `E052` shouldn't detect any errors - in this case we can just clear the `expected` map to make sure it doesn't contain any expected errors: 

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        ...        
        
        expected.clear(); // We don't expect any errors, so clear the map to make sure it's empty
        
    }

Finally, we can use the `TestUtils` class to help assert that out actual output of the validation rule matches the expected output:

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        ...
        
        TestUtils.assertResults(expected, results); // Make sure that the actual output matches the expected output

    }

You can now run the above unit test, and it should pass.

Now, we need to add another vehicle to the feed so there is a conflicting ID - this should trigger a single occurrence of rule E052.  Note that when adding the 2nd entity we need to use `feedMessageBuilder.addEntity(1` instead of `feedMessageBuilder.setEntity(1` because a 2nd entity doesn't yet exist in the message builder.

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        ...
        
        // Add a 2nd VehiclePosition entity with the same vehicle ID of 1
        GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilderConflict = GtfsRealtime.VehicleDescriptor.newBuilder();
        vehicleDescriptorBuilderConflict.setId("1");

        vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
        feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
        feedMessageBuilder.addEntity(1, feedEntityBuilder.build()); // We need to use "addEntity" instead of "setEntity" here so that a 2nd entity is added to the feed.

    }

Now we need to run validation on this test data, set up the expected output to be 1 occurrence of E052, and make sure our expected output matches the actual output:

    /**
     * E052 - vehicle.id is not unique
     */
    @Test
    public void testE052() {
        ...

        // 1 error for duplicate vehicle ID of 1
        results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
        expected.put(E052, 1); // There is one expected error for E052
        TestUtils.assertResults(expected, results);

        clearAndInitRequiredFeedFields(); // All unit tests should end with this method to make sure all the objects are re-initialized for the next test
    }

That's it for this unit test!  Here's the full unit test that includes all of the above code:

     /**
      * E052 - vehicle.id is not unique
      */
     @Test
     public void testE052() {
         VehicleValidator vehicleValidator = new VehicleValidator();
         Map<ValidationRule, Integer> expected = new HashMap<>();
     
         // Set up the test data for a scenario with no errors (no vehicles with duplicate IDs)
         GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilder = GtfsRealtime.VehicleDescriptor.newBuilder();
         vehicleDescriptorBuilder.setId("1");
     
         vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
         feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
         feedMessageBuilder.setEntity(0, feedEntityBuilder.build());
     
         // Run validation using the VehicleValidator
         results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
         // We aren't expecting any errors, so clear the expected errors data structure
         expected.clear();
         // Compare the expected results to the actual results of validation
         TestUtils.assertResults(expected, results);
     
         // Set up the test data for a scenario with one error (ID "1" is duplicated in Entity 0 and Entity 1)
         GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptorBuilderConflict = GtfsRealtime.VehicleDescriptor.newBuilder();
         vehicleDescriptorBuilderConflict.setId("1");
     
         vehiclePositionBuilder.setVehicle(vehicleDescriptorBuilder.build());
         feedEntityBuilder.setVehicle(vehiclePositionBuilder.build());
         feedMessageBuilder.addEntity(1, feedEntityBuilder.build());
     
         // Run validation using the VehicleValidator
         results = vehicleValidator.validate(MIN_POSIX_TIME, bullRunnerGtfs, bullRunnerGtfsMetadata, feedMessageBuilder.build(), null, null);
         // Set up expected results of 1 error for E052
         expected.put(E052, 1);
         // Confirm that the validator returned one error for E052
         TestUtils.assertResults(expected, results);
     
         clearAndInitRequiredFeedFields(); // All unit tests should end with this method to make sure all the objects are re-initialized for the next test
     }

Note that in some scenarios you may create sample test data to evaluate a new rule that also triggers errors generated by other rules.  In that case, if this is expected behavior based on the rules, you can just add multiple entries to the `map`, one for each rule->count mapping.  For example:

        // Expect 1 occurrence of E052 and 1 occurrence of E053 
        expected.put(E052, 1); 
        expected.put(E053, 1);

But hold on, we're not done yet!

You'll need to update the test that checks for the total number of expected rules in `UtilTest.testGetAllRules()`:

    @Test
    public void testGetAllRules() {
        List<ValidationRule> rules = ValidationRules.getRules();
        assertEquals(61, rules.size()); // < -- Increment the expected value by one to include the new rule
    }

Now you'll need to run all unit tests to see if they pass.  Related to the above mentioned scenario - sometimes you will add a new rule that may be triggered by test data when unit testing other rules in the same class (e.g., `VehicleValidator`), causing those unit tests for other rules to fail when you run them.  For example, if you test rule E002 that checks for strictly sorted stop_time_updates by stop_sequence and confirm that it generates an error when stop_sequence isn't supplied, if you test this on a dataset that has a loop route it will also trigger an occurrence of rule E009, which requires stop_sequence for all loop routes.

You can fix other failing unit tests by adding an occurrence of the new rule - for example this:

        expected.put(E002, 1); 
        TestUtils.assertResults(expected, results);

...would become:

        expected.put(E002, 1);
        expected.put(E009, 1); 
        TestUtils.assertResults(expected, results);
        
Before fixing other unit tests that start failing after you add a new test, it's important to make sure that you understand the rule that started failing as well as the input data to make sure that this is expected behavior, and that you didn't accidentally introduce a bug that's causing the other test to fail.

That's it, you're all done!  You can take a look at a complete set of changes that implement the new rule E052 in [this commit on Github](https://github.com/CUTR-at-USF/gtfs-realtime-validator/commit/121173f5167cc0d460c3eb50b3582265470671c4).
