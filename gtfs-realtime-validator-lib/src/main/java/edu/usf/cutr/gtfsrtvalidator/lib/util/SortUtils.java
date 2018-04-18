/*
 * Copyright (C) 2017 University of South Florida
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.usf.cutr.gtfsrtvalidator.lib.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Utility methods to sort things
 */
public class SortUtils {

    /**
     * Sorts the provided list of files by date modified (ascending)
     *
     * @param files the list of files to sort by date modified (ascending)
     * @return the provided list of files sorted by date modified (ascending)
     */
    public static File[] sortByDateModified(File[] files) {
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        return files;
    }

    /**
     * Compares the value of two Path objects for order by date modified
     *
     * @param o1 first path to compare
     * @param o2 second path to compare
     * @return 0 if o1 date modified is equal to o2, a value less than 0 if o1 has a date
     * modified that is before o2, and a value greater than 0 if o1
     * represents a date modified that is after o2
     */
    public static int compareByDateModified(Path o1, Path o2) throws IOException {
        return Long.compare(Files.getLastModifiedTime(o1).toMillis(), Files.getLastModifiedTime(o2).toMillis());
    }

    /**
     * Sorts the provided list of files by name (ascending)
     *
     * @param files the list of files to sort by name (ascending)
     * @return the provided list of files sorted by name (ascending)
     */
    public static File[] sortByName(File[] files) {
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    /**
     * Compares the value of two Path objects for order by file name
     *
     * @param o1 first path to compare
     * @param o2 second path to compare
     * @return 0 if o1 name is equal to o2 name, a value less than 0 if o1 has a name that
     * comes before the name of o2, and a value greater than 0 if o1
     * has a name that is after o2
     */
    public static int compareByFileName(Path o1, Path o2) {
        return o1.getFileName().toString().compareTo(o2.getFileName().toString());
    }
}
