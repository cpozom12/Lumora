# CPZ Media Hub — standalone provenance boundary

## Historical workshop

The repository `cpozom12/Lumora` began as a fork of the MIT-licensed Lumora project. The upstream license remains applicable to upstream-derived files retained in the historical `app/` tree and related fork history. Nothing in this document removes or overrides upstream copyright.

## Permanent CPZ product

`standalone/CPZMediaHub/` is the permanent CPZ Media Hub implementation. It was written as a separate implementation from the product requirements established during the workshop rather than by copying the upstream application source tree.

Permanent-project characteristics:

- independent Gradle root named `CPZMediaHub`;
- package/application ID `com.cpozom.mediahub`;
- Java/Android platform APIs only;
- no upstream package namespace;
- no upstream source files, resources, logos, strings, libraries, scrapers, plugins, torrent/P2P engine or media player stack;
- no runtime third-party dependencies;
- no Android permissions;
- original CPZ icon and UI;
- original CPZ copyright notice;
- static use of public Android package identifiers solely to hand off to official installed applications.

The standalone security gate fails if upstream-name markers or the retired runtime stacks reappear in executable source or the built APK.

## Licensing rule going forward

Do not copy upstream-derived source or assets from the historical fork into `standalone/CPZMediaHub/` without a deliberate license review. If a future change incorporates a copy or substantial portion of MIT-licensed upstream software, preserve the applicable upstream copyright and MIT permission notice as required by that license.

The historical fork may continue to exist for audit/provenance purposes without being part of the permanent CPZ Media Hub distribution.
