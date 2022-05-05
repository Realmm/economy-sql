# Simple Economy Plugin with SQL

### Installation

- Server version is paper-1.18.1, ensure this is the jar you are using
- Considering this, ensure you are also using Minecraft version 1.18.1
- Install to /plugins folder in the server directory
- Run the plugin once, then edit the configuration with suitable values. You only need to edit
  'sql.username' and 'sql.password' if the SQL server is on the same host as the server

### Configuration

- sql.host - The host to connect to, defaults to 'localhost'
- sql.database - The database name to create then connect to, defaults to 'economyDatabase'
- sql.username - The SQL username of the user connecting
- sql.password - The SQL password of the user connecting

### Commands

- /economy - Player only command, shows your balance
- /economy balance [player] (or /balance [player]) (alias 'bal') - Show a certain players balance
- /economy setbalance [player] [value] (or /setbalance [player] [value]) (alias 'setbal') - 
  Set a certain players balance to a whole, positive value. Admin only command, permission 'economy.admin'

### Plugin Quirks

- A players balance will not have loaded until they have logged in at least once during the server lifetime
- Once a player logs in, their balance is loaded from the database
- Updating the players balance does not directly update the database, if the server crashes the players balance
  will not be saved accurately. Players balance is also not saved on shutdown of the plugin. A players balance
  is only saved when the player logs out, which would likely lead to inaccuracies 
- Considering the database isn't updated directly upon balance update, if attempting to update an offline players 
  balance, it won't save to the database until the player logs in, and when they log in it grabs the current value 
  from the database, disregarding what was set when the player was offline, which is inaccurate due to plugin design 
  as well
- All plugin 'quirks' are intentional, and can be resolved with relative ease
