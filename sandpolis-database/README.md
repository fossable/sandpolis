## `sandpolis-database`

This layer implements the Sandpolis data model which is fundamental to all other
layers. All instances maintain their own database for different reasons:

- The server's database persists data for the entire network for long periods of
  time
- The client's database caches data fetched from the server temporarily while
  the user interacts with the application
- The agent's database caches data before it's sent to the server

### `Data` objects

Entries in the database (uncreatively called `Data`) are defined by Rust
structs:

```rs
#[derive(Serialize, Deserialize, PartialEq, Debug, Clone, Data)]
#[native_model(id = 15, version = 1)]
#[native_db]
pub struct ExampleData {
    #[primary_key]
    pub _id: DataIdentifier,

    pub value: u32,
}
```

In the database, `Data` structs are stored as key-value pairs.

#### Resident `Data`

Certain `Data` may be brought into memory for ease of use and faster access.
There are two types to simplify this:

- `Resident`
- `ResidentVec`
