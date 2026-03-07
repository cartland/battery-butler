# Code Share Analysis

This document provides a breakdown of the codebase by application layer and module.
Total Lines of Code: 35280

## Application Breakdown
* Shared Code: 24,592 lines (69.7%)
* iOS Swift App: 3,079 lines (8.7%)
* Other: 2,929 lines (8.3%)
* CMP Android, iOS, Desktop: 2,169 lines (6.1%)
* Server: 1,528 lines (4.3%)
* Android Screenshot Tests: 983 lines (2.8%)

## Module Breakdown
* `:presentation-feature`: 5,730 lines (16.2%)
* `:viewmodel`: 4,566 lines (12.9%)
* `:usecase`: 4,073 lines (11.5%)
* `:iosAppSwiftUI.xcodeproj`: 3,079 lines (8.7%)
* `:data-network`: 2,412 lines (6.8%)
* `:domain`: 2,293 lines (6.5%)
* `:compose-app`: 2,169 lines (6.1%)
* `:presentation-core`: 2,095 lines (5.9%)
* `:data`: 1,801 lines (5.1%)
* `:buildSrc`: 1,365 lines (3.9%)
* `:server:app`: 1,342 lines (3.8%)
* `:data-local`: 1,059 lines (3.0%)
* `:android-screenshot-tests`: 983 lines (2.8%)
* `:test-common`: 609 lines (1.7%)
* `:ai`: 493 lines (1.4%)
* `:e2e-tests`: 271 lines (0.8%)
* `:ios-swift-di`: 235 lines (0.7%)
* `:fixtures`: 209 lines (0.6%)
* `:presentation-model`: 209 lines (0.6%)
* `:server:data`: 152 lines (0.4%)
* `:compose-resources`: 70 lines (0.2%)
* `:server:domain`: 34 lines (0.1%)
* `:iosAppComposeUI.xcodeproj`: 31 lines (0.1%)

## Code Distribution

```mermaid
---
config:
  sankey:
    showValues: true
    width: 800
    height: 1000
    nodeAlignment: justify
    linkColor: gradient
---
sankey-beta

Codebase,Shared Code,24592
Codebase,iOS Swift App,3079
Codebase,Other,2929
Codebase,CMP Apps,2169
Codebase,Server,1528
Codebase,Screenshot Tests,983

Shared Code,presentation-feature,5730
Shared Code,viewmodel,4566
Shared Code,usecase,4073
Shared Code,data-network,2412
Shared Code,domain,2293
Shared Code,presentation-core,2095
Shared Code,data,1801
Shared Code,data-local,1059
Shared Code,ai,493
Shared Code,compose-resources,70

iOS Swift App,iosAppSwiftUI,3079

Other,buildSrc,1365
Other,test-common,609
Other,e2e-tests,271
Other,ios-swift-di,235
Other,fixtures,209
Other,presentation-model,209
Other,iosAppComposeUI,31

CMP Apps,compose-app,2169

Server,server:app,1342
Server,server:data,152
Server,server:domain,34

Screenshot Tests,android-screenshot-tests,983

```
