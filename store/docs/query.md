Query Syntax
============

````
  content:(TEXT)
  subject:[>,<,>=,<=](TEXT)
  msgid:(TEXT) // Message-Id: field from mime header
  envto:(TEXT|EMAIL_ADDR|DOMAIN) // x-envelope-to mime header
  envfrom:(TEXT|EMAIL|DOMAIN) // x-envelope-from mime-header
  contact:(TEXT) // special-case searching for contact picker (matches type=contact documents only)
  to:(TEXT|EMAIL_ADDR|DOMAIN)
  from:[>,<,>=,<=]({TEXT}|{EMAIL_ADDR}|{DOMAIN})
  cc:(TEXT|EMAIL|DOMAIN)
  tofrom:(TEXT|EMAIL|DOMAIN) // TO or FROM
  tocc:(TEXT|EMAIL|DOMAIN) // TO or CC
  fromcc:(TEXT|EMAIL|DOMAIN) // TO or FROM or CC
  tofromcc:(TEXT|EMAIL|DOMAIN) // TO or FROM or CC
  in:(FOLDER_LABEL) // in the specified folder
  under:(FOLDER_LABEL) // in the specified folder and all descendants
  inid:(FOLDER_ID) // in the specified folder
  underid:{FOLDER_ID} // in the specified folder and all descendants
  has:(attachment|OBJECT_TYPE)
  filename:(TEXT)
  type:(RAW_MIME_TYPE|FRIENDLY_MIME_TYPE)
  attachment:(RAW_MIME_TYPE|FRIENDLY_MIME_TYPE)
  is:(anywhere|unread|read|flagged|unflagged|sent|received|replied|unreplied|
      forwarded|unforwarded|invite|solo|tome|fromme|ccme|tofromme|fromccme|
      tofromccme|local|remote)
  date:[>,<,>=,<=](DATE) // created date
  mdate:[>,<,>=,<=](DATE) // modified date
  day:[>,<,>=,<=](DATE)
  week:[>,<,>=,<=](DATE)
  month:[>,<,>=,<=](DATE)
  year:[>,<,>=,<=](DATE)
  after:(DATE)
  before:(DATE)
  size:([<>])?(SIZE)
  bigger:(SIZE)
  larger:(SIZE)
  smaller:(SIZE)
  tag:(TAG)
  priority:(high|low)
  message:(DB_MSG_ID)
  my:(MY_SAVED_SEARCH_NAME) // not supported yet
  modseq:[>,<,>=,<=](CHANGE_ID)
  conv:(DB_CONV_ID)
  conv-count:(NUM)
  conv-minm:(NUM)
  conv-maxm:(NUM)
  conv-start:(DATE)
  conv-end:(DATE)
  appt-start:[>,<,>=,<=](DATE)
  appt-end:[>,<,>=,<=](DATE)
  author:(TEXT)
  title:(TEXT)
  keywords:(TEXT)
  company:(TEXT)
  metadata:(TEXT)
  item:(all|none|[0-9]+|{[0-9]+(,[0-9]+)*}|{[0-9]+--[0-9]+})
  field[FIELDNAME]:(TEXT)|[>,<,>=,<=](NUMBER)
  #FIELDNAME:(TEXT)|[>,<,>=,<=](NUMBER)
  sort: overrides the sort field specified in the <SearchRequest>

FRIENDLY_MIME_TYPE:"text"|"application"|"word"|"msword"|"excel"|"xls"|"ppt"|"pdf"|"ms-tnef"|"image"|"jpeg"|"gif"|"bmp"|"none"|"any"
TEXT: text string, must be in "'s if has spaces in it
EMAIL_ADDR: text string, no spaces, with @ sign
DOMAIN: such as *.com
FOLDER_LABEL: mail|trash|spam|anywhere
TAG: tag_name
OBJECT_TYPE: "phone" "url" "credit-card" etc...types of parsed objects
DATE: absolute-date = mm/dd/yyyy (locale sensitive) OR
      relative-date = {+|-}nnnn{mi|minute[s]|h|hour[s]|d|day[s]|w|week[s]|m|month[s]|y|year[s]}
SIZE: n+{b,kb,mb,gb}    // default is b
DB_MSG_ID: ??
NUM: ([<>])?n+
````

Test Cases
==========

````
ski
after:3/1/2004
subject:linux
subject:"code has"
linux or has:ssn
larger:1M
is:flagged
not is:flagged
not in:junk
-is:read
````

`-` is a synonym for `not` and may immediately precede a field.

Fields
======

CONTENT field
-------------

CONTENT field (e.g. email message body) is tokenized by word. We deem the following Unicode ranges are CJK characters.

 * 2E80-2EFF CJK Radicals Supplement
 * 2F00-2FDF Kangxi Radicals
 * 2FF0-2FFF Ideographic Description Characters
 *------------------------------------------------------------------------------
 * 3000-303F [EXCLUDE] CJK Symbols and Punctuation
 *------------------------------------------------------------------------------
 * 3040-309F Hiragana
 * 30A0-30FF Katakana
 * 3100-312F Bopomofo
 * 3130-318F Hangul Compatibility Jamo
 * 3190-319F Kanbun
 * 31A0-31BF Bopomofo Extended
 * 31C0-31EF CJK Strokes
 * 31F0-31FF Katakana Phonetic Extensions
 * 3200-32FF Enclosed CJK Letters and Months
 * 3300-33FF CJK Compatibility
 * 3400-4DBF CJK Unified Ideographs Extension A
 * 4DC0-4DFF Yijing Hexagram Symbols
 * 4E00-9FFF CJK Unified Ideographs
 *------------------------------------------------------------------------------
 * AC00-D7AF Hangul Syllables
 * D7B0-D7FF Hangul Jamo Extended-B
 *------------------------------------------------------------------------------
 * FF00-FFEF Halfwidth and Fullwidth Forms

 Additionally, the following Unicode ranges of Southeast Asian languages are recognized:
 
 * 0E00-0E7F Thai
 * 0E80-0EFF Lao
 * 1000-109F Myanmar
 * 1780-17FF Khmer

To CJK character sequences, we apply the bigram tokenization where words are constructed by every subsequence of 2
characters (e.g. ABCD consists of AB, BC and CD) regardless of the grammatical structure. Therefore, CJK character
sequences are searchable by a subsequence of 2 characters or a combination of those. For example, suppose you have
a text content: "ABCDEFG". Searching by "AB", "BC", "BCD", "BCDE" are all hits even if "ABCDEFG" are grammatically
tokenized into "ABC" "DEF", and "G".

Thai character sequences get tokenized using a dictionary-based tokenizer that tries to find word breaks despite
the lack of whitespace. Common thai stopwords are then removed.

All other character sequences are split by whitespace [\r|\n|\t|\f] or punctuation [/|.|,] with some exceptions:

 * We recognize email addresses by a pattern of "*@*.*[.*]".
 * We recognize host names by a pattern of "*.*[.*]".
 * We recognize numbers by a pattern of "*[0-9]([_|-|.|,][0-9])*".
 * Characters in the set + - < > are treated as literals when they are present in a string of at least one 
   alphanumeric or CJK character, but ignored otherwise. For example, "c++" is kept intact but just "++" is skipped.
 * Character sequences from the set ~ ! # $ % ^ & ( ) ? { } [ ] ; form their own tokens.
   For example, "foo!?bar" gets tokenized into "foo", "!?", and "bar". Punctuation characters do not impede phrase searches,
   so "foo bar" will match "foo!?bar". 
 
After tokenization, all words become case-insensitive, and the following common English words are trimmed.

"a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on",
 "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with"

Therefore, non CJK character sequences are only searchable by a word or a combination of words. For example, suppose
you have a text content: "3401 Hillview Ave, Palo Alto, CA 94304 USA". Searching by "3401", "ave" and "palo alto" are
all hits. But, substrings of each word such as "hill", "view", "94" or "US" are not hits.

Wildcards (*) are supported at the end of any word in a query, or at the beginning of a single-word query.
 
To demonstrate this, the following types of wildcard queries are valid:

    foo*
    foo bar*
    foo* bar
    foo* bar*
    *foo

But the following are not:

    *foo bar
    foo *bar

Stopwords in queries are ignored, and matching phrases can have any term in place of the stopword.
This means that a query of the form "foo <STOPWORD> bar" will match "foo <ANY WORD> bar".

CONTACT field
-------------

CONTACT field works in the same manner as CONTENT field besides it's also seachable by stop words and always translated
to a prefix search.

header-related

    from:
    to:
    cc:{name|domain}
    subject:

subject searches support the range modifiers <, >, <=, >= at the beginning of the query:

    * subject:<foo
    * subject:>="foo bar"
 
However, inside parentheses these characters are treated as literals:

    * subject:"<foo"
    * subject:">=foo bar" 

saved-search-related (UNIMPLEMENTED)
------------------------------------

    my:{name-of-saved-search}

i.e., could have a saved search of "tofromcc:example.zimbra.com"
called "zimbra" and say:

    my:zimbra

object-related fields:

    has:attachment              constrains search to messages with attachments
    has:{phone|url|ssn|po...}   messages with "objects"

mime-related

    filename:{file-name}        constrains search to messages with attachments of given name
    type:{mime-type}            constrains search to blobs of the given type
    attachment:{mime-type}      constrains search to messages with attachments of the given type

    mime-type = raw-mime-type | mime-type-alias
    raw-mime-type = type/sub-type (i.e., text/plain)
    # aliases are "user-friendly" mime types
    mime-type-alias = "word" | "excel" | "pdf", "image", "audio", etc...

EXAMPLES:

    type:word "hello"             searches within only words docs for "hello"
    attachment:word "hello"       searches within messages that have word docs for "hello"
    attachment:image/*            matches all messages that have image attachments

flags

    is:anywhere --> in any folder (overrides spam-trash setting for that query part)
                    note that "is:anywhere" does NOT imply "across all mountpoints".
                    Mountpoints must be explicitly included with an "in:" term --
                    in:mountpointname.
    is:unread
    is:read
    is:flagged
    is:unflagged
    is:sent
    is:received
    is:invite
    is:solo --> true if the item has no conversation

The flag name can also be used in a search:

    tag:\{FlagName} where {FlagName} is one of the following values:
        Attached, Answered, Copied, Deleted, Draft, Flagged, Forwarded, Notified, Subscribed, Unread

date-related fields

    after:{date}
    before:{date}
    date = {absolute-date} | {relative-date}

    absolute-date = mm/dd/yyyy (locale sensitive)

    relative-date = [+/-]nnnn{minute,hour,day,week,month,year}

NOTE: absolute-date is locale sensitive. Our implementation delegates it to
      JDK's DateFormat class whose behavior is as follows:

  * ar - dd/mm/yyyy
  * be - dd.mm.yyyy
  * bg - yyyy-mm-dd
  * ca - dd/mm/yyyy
  * cs - dd.mm.yyyy
  * da - dd-mm-yyyy
  * de - dd.mm.yyyy
  * el - dd/mm/yyyy
  * en - mm/dd/yyyy (default)
  * en_AU - dd/mm/yyyy
  * en_CA - dd/mm/yyyy
  * en_GB - dd/mm/yyyy
  * en_IE - dd/mm/yyyy
  * en_IN - dd/mm/yyyy
  * en_NZ - dd/mm/yyyy
  * en_ZA - yyyy/mm/dd
  * es - dd/mm/yyyy
  * es_DO - mm/dd/yyyy
  * es_HN - mm-dd-yyyy
  * es_PR - mm-dd-yyyy
  * es_SV - mm-dd-yyyy
  * et - dd.mm.yyyy
  * fi - dd.mm.yyyy
  * fr - dd/mm/yyyy
  * fr_CA - yyyy-mm-dd
  * fr_CH - dd.mm.yyyy
  * hr - yyyy.MM.dd
  * hr_HR - dd.MM.yyyy.
  * hu - yyyy.MM.dd.
  * is - dd.mm.yyyy
  * it - dd/mm/yyyy
  * it_CH - dd.mm.yyyy
  * iw - dd/mm/yyyy
  * ja - yyyy/mm/dd
  * ko - yyyy. mm. dd
  * lt - yyyy.mm.dd
  * lv - yyyy.dd.mm
  * mk - dd.mm.yyyy
  * nl - dd-mm-yyyy
  * nl_BE - dd/mm/yyyy
  * no - dd.mm.yyyy
  * pl - yyyy-mm-dd
  * pl_PL - dd.mm.yyyy
  * pt - dd-mm-yyyy
  * pt_BR - dd/mm/yyyy
  * ro - dd.mm.yyyy
  * ru - dd.mm.yyyy
  * sk - dd.mm.yyyy
  * sl - dd.mm.yyyy
  * sq - yyyy-mm-dd
  * sv - yyyy-mm-dd
  * th - dd/mm/yyyy
  * tr - dd.mm.yyyy
  * uk - dd.mm.yyyy
  * vi - dd/mm/yyyy
  * zh - yyyy-mm-dd
  * zh_TW - yyyy/mm/dd

In case of format error, it falls back to mm/dd/yyyy.

NOTE: need to figure out how to represent "this week", "last week", "this month", etc. probably
some special casing of relative dates and use with after/before. i.e., maybe "after:-2d AND before:0d" means
yesterday? i.e., for relative day/week/month/year, you zero out month/week/day/hour/minute?

  * last 4 hours:   after:-4hour
  * today:          after:0day
  * yesterday:      (after:-2day AND before:0day)
  * this week:      after:0week
  * last week:      (after:-2week AND before:0week)
  * this month:     after:0month
  * last month:     (after:-2month AND before:0month)
  * this year:      after:0year
  * last year:      (after:-2year AND before:0year)
  * last year and older:  before:0year

appointment search operators
============================

appt-start:  appt-end:
----------------------

Search based on the start and end times of the appointment.
For non-recurring appointments, this is basically what you
expect.  For recurring appointments, the start and end times are
the *earliest possible* time (start of the first instance in the
recurrence) and *latest possible* time, or sometime in 2099 if
the recurrence has no end.

size-releted fields

    larger:{size}
    smaller:{size}
    size:([<>])?{size}

    size is nnnn{b,kb,mb,gb}    # default is b


tag-related fields

    tag:{user-defined-tag}


domain-related fields

operators supporting domain searches are:

   * to
   * from
   * cc
   * tofrom
   * tocc
   * fromcc
   * tofromcc

<domain operator>:{domain-list}

EXAMPLES: `tofromcc:stanford.edu` OR: `from:*.org`

db-related fields

    message:{db-message-id}        # constrain searches to a particular message

conversation-related-fields:
  * conv:{db-conv-id}      # constrain searches to a paritcular conversation
  * conv-min-count:{num}   # constrain searches to conversations of a particular length
  * conv-max-count:{num}   # constrain searches to conversations of a particular length
  * conv-start:{date}
  * conv-end:{date}

metadata-related fields

  * author:
  * title:
  * keywords:
  * company:
  * metadata:

The metadata fields refer to the metadata of a non-textual attachment.
The fields author, title, keywords, company refer to the metadata fields
of the same name in the document.
The field metadata aggregates all the metadata fields including the above four.
E.g.,

    author:acme     finds all attachments whose author is acme
    metadata:acme   finds all attachments where acme appears in any metadata
                    fields, including author.

misc fields

  *  minm:nnnn                     # constrain to conversations with at least nnnn messages
  *  maxm:nnnn                     # constrain to conversations with at most nnnn messages


other-mime-specific fields

how do we want to handle doc properties from word, pdf, mp3, etc?

i.e.:

    genre:rock OR artist:rush
    title:"customer visit"
    keywords:security
    author:ross

* maybe {mime-type-alias}.field? i.e.:

    audio.genre:rock OR audio.artist:rush   (or mp3.*?)
    word.title:hello

where the mime-type-alias can be left off if field is non-ambigious?

do we want to try and promote certain fields that we can share between mulitple types? (title, author, keywords)


Structured-Data Searching
=========================

Search/Indexing now has the ability to analyze and store data in
Name-Value pairs in such a way that they can be searched for in a
structured way by the query language.

For example, Contact name-value pairs are indexed this way.

Structured data is stored in the "l.field" lucene field, and it should be added to the index document in a format like this:

    "fieldOne:value1 value2 value3 value4\nFieldTwo:value2 value3 value4"

The search language has been extended to allow field searches to be expressed like this:

    #FieldName:value

For example, to find a contact with the last name "Davis" you would use a search query like:

    #lastname:davis

OR

    FIELD[lastname]:davis
