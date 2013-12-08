# cronj

A simple to use, cron-inspiried task scheduler

[![Build Status](https://travis-ci.org/zcaudate/cronj.png?branch=master)](https://travis-ci.org/zcaudate/cronj)

## Installation:

In project.clj, add to dependencies:

     [im.chit/cronj "1.0.1"]

All functions are in the `cronj.core` namespace.

### Documentation

See main site at:

http://docs.caudate.me/cronj/

To generate this document for offline use: 

  1. Clone this repository
  
      > git clone https://github.com/zcaudate/cronj.git
  
  2. Install [lein-midje-doc](http://docs.caudate.me/lein-midje-doc). 
  
  3. Create `docs` folder
      > mkdir docs

  4. Run in project folder
  
      > lein midje-doc

The output will be generated in `docs/index.html`


## License
Copyright © 2013 Chris Zheng

Distributed under the MIT License
