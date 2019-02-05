## Github client Android Java sample showing instant search and pagination.

Utilizes Android JetPack Architecture Components: Paging Library, ViewModel, LiveData.  And also utilizes Retrofit2 and RxJava.  This is a network only sample, there's no persistent storage.

## RepoViewModel

- Initializes the Data Source Factory.

- Initializes the LiveData objects that the main activity observes. 

- Initializes the RxJava publishers: one for the initial request and another for the pagination request.

- Initializes Retrofit2 and the Github Service API.

- Fetches the Github Repo data.


## RxJava

### Initiator - PublishSubject

- Debounce: The debounce operator is used to reduce requests frequency on key input.

- Filter: The filter operator is used to reduce requests until there is more than 2 characters.

- DistinctUntilChanged: The distinctUntilChanged operator is used to avoid the duplicate network calls and ignore making same requests.

- SwitchMap: The switchMap operator is used to discard ongoing requests and return only the latest response. It only provides the result for the last search query(most recent) and ignores the rest.


### Paginator - PublishProcessor

- BackpressureDrop: The backpressureDrop operator is used to drop requests if it can't handle more than it's capacity 128 requests.

- ConcatMap - The concatenateMap operator is used to handle the output of multiple observables to act like a single observable and preserves the order of the requests.
  

## Screenshot

![](docs/screenshot.png)


## License

Copyright 2018 Brian Lichtenwalter

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

