# Instance

The following sections apply to all Sandpolis instances.

## Data Model

There are three layers in the Sandpolis data model. Of which, client
implementations are required to support at least two (ST and OID layers).

### The ST Layer

The State Tree layer is the lowest layer and is concerned with storage and
persistence. Every instance maintains a global tree called the "ST Tree". The
tree is seldomly manipulated directly. Instead, higher layers make changes to
the ST Tree on behalf of consumers.

The ST tree is composed of two components: `Attribute`s and `Document`s.

#### Attributes

Attributes contain data of a specific type and meaning. All data in the ST tree
is stored in attributes.

##### Retention

The history of an attribute can optionally be recorded with _tracked
attributes_.

##### AttributeChangedEvent

#### Documents

Documents are a set of attributes and sub-documents.

##### DocumentAddedEvent

Indicates that a document has been added to the tree. No futher events will be
fired for all children of the added document as a direct result of the addition.

##### DocumentRemovedEvent

Indicates that a document has been removed from the tree.

### The OID Layer

Every node in a ST Tree is uniquely identified by an OID.

#### Path

The OID path is a sequence of `/` separated strings that describe how to reach
the corresponding node from the root node.

Elements of the path are called _components_ which may consist of any number of
alphanumeric characters and underscores. If a component equals the wildcard
character (`*`), then the OID corresponds to all possible values of that
component and is known as a _generic_ OID. If an OID is not generic, then it's
_concrete_.

#### Namespace

OIDs have a namespace string that identifies the module that provides the OID.
This allows modules to define OIDs without the possibility of collisions. The
namespace string must equal the name of the module that defines an OID.

Namespace notation is to prefix the namespace string and a `:`, similar to the
protocol section of a URI:

```
com.sandpolis.plugin.example:/profile/*/example
```

#### Temporal Selector

In order to select historic values of an attribute, concrete OIDs may include a
timestamp range selector or an index selector.

##### Timestamp Selector

To select all values within an arbitrary timestamp range, specify the inclusive
start and end epoch timestamps separated by a `..` in parenthesis. If either
timestamp is omitted, then the range is extended to the most extreme value
possible.

```
/profile/ba4412ea-1ec6-4e76-be78-3849d2196b52/example(1628216870..1628216880)
```

##### Index Selector

To select an arbitrary amount of values, specify inclusive start and end
indicies separated by a `..` in square brackets. If either index is omitted,
then the range is extended to the most extreme value possible. Index 0 is the
oldest value.

```
/profile/ba4412ea-1ec6-4e76-be78-3849d2196b52/example[2..7]
```

To select one value, omit the range specifier entirely:

```
/profile/ba4412ea-1ec6-4e76-be78-3849d2196b52/example[1]
```
