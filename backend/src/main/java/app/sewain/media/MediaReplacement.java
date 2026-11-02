package app.sewain.media;

/** Finish an in-place file replacement after the metadata transaction completes. */
interface MediaReplacement {
    void complete(boolean committed);
}
