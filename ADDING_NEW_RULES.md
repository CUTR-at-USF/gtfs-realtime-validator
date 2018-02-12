# Adding new rules

We will want to add new rules to this validator as the [GTFS-realtime spec](https://github.com/google/transit/tree/master/gtfs-realtime) and the surrounding applications and tools change.  This page outlines the process of adding new rules to this tool

### 0. - Prepare for implementation 

* Check the list of [currently implemented rules](RULES.md) to make sure the rule doesn't already exist.
* Check the list of [planned future rules](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22) to see if an issue already exists for the proposed rule.
    * If no existing issue exists, open a new issue with the ["new rule" label](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues?q=is%3Aissue+is%3Aopen+label%3A%22new+rule%22).
* Discuss the rule with the community via the Github issue and come to a general consensus on the exact logic, and if it should be an `ERROR` or `WARNING`.  Generally, errors are behavior that directly violate the GTFS-realtime documentation.  Warnings are behavior that is not advised (e.g., against best practices) but not explicitly forbidden in the GTSF-realtime documentation.
* Implement new rule using the process below

For the below example, let's look at implementing a new rule that will make sure that each `vehicle.id` in a VehiclePositions feed is unique.  If there are two VehiclePosition entities in a feed with the same `vehicle.id`, then the validator should log an error.

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
*  `HeaderValidator` - Examines the GTFS-realtime header
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

### 4. Add the validation logic for the new rule

This exact process will differ for each rule, but first let's cover some of the basics that are the same across any rule implementation in the `*Validator.validate()` method.

First, at the beginning of the `*Validator.validate()` method you'll need to declare a list to hold occurrences of an error:
 
 ~~~
     public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
         List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
         List<OccurrenceModel> e026List = new ArrayList<>();
         List<OccurrenceModel> e027List = new ArrayList<>();
         List<OccurrenceModel> e028List = new ArrayList<>();
         List<OccurrenceModel> e029List = new ArrayList<>();
         List<OccurrenceModel> w002List = new ArrayList<>();
         List<OccurrenceModel> w004List = new ArrayList<>();
         List<OccurrenceModel> e052List = new ArrayList<>(); // <--- Add this
~~~

And, at the end of the `*Validator.validate()` method you'll need to add this list of E052 occurrences to the list of all errors/warnings that are returned by this `*Validator`:

~~~
        if (!w002List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W002), w002List));
        }
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
