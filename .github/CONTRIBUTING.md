## Setting up the project

Use JDK 25 and clone the JS backend next to this repository:

```bash
git clone https://github.com/Melationin/JS-backend.git ../js-backend
./gradlew build
```

Build outputs are written to `build/libs` and `dist`.

## PR Guidelines

This is mainly for my sanity and so that your PRs can be merged faster.

* try not to mess with the style too much, it can make backporting more difficult
    * if you *only* change the style in a file, DONT.
* target the current main branch
* split PRs up into managable sections by feature
    * I don't want to see PR like [#71](https://github.com/JsMacros/JsMacros/pull/71) where there's a ton of different
      stuff in it.
    * PRs should be for a **SINGLE** feature, or related group of features (ie. a new library)
