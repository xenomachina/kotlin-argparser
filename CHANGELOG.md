# 1.1.0

  - Adds auto-naming of options and positionals.

    - Each of the ArgParser methods that takes names and returns a Delegate<T> has
      an overload that takes no name, but returns a DelegateProvider<T>.

    - A DelegateProvider<T> has an `operator fun provideDelegate` that returns a
      Delegate<T>, using a name derived from the name of the property the
      DelegateProvider is being bound to.

  - Bug fixes:

    - Removed option from README.md, as it is internal

    - Fixed spelling of addValidator. "addValidtator" is still there, but
      deprecated. It'll probably be removed in the next release, barring the
      addition of potato functionality.

# 1.0.2

  - Upgrade to Kotlin 1.1, extract xenocom package

# 1.0.1

  - Fix small bug where `runMain` didn't have a default value for `columns`
    parameter. (Now defaults to null.)

# 1.0.0

  - Initial release
