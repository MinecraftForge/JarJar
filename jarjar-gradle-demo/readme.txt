Basic layout of the subprojects:

root:
  nested-outer:
    dep
    nested-inner
      dep2
  
  // These are designed to build a jar that can be tossed into a ForgeDev mods folder. We we use a real-world dependency
  mod-outer:
    mixinextras-forge v0.4.0
    mod-inner:
      mixinextras-forge v0.4.1