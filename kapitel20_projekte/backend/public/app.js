// Läuft im Browser: spricht nur mit dem Backend – ohne Token, ohne Bibliothek
const kacheln = document.querySelector("#kacheln");

function zeige(item) {
  let k = document.getElementById(item.name);
  if (!k) {
    k = document.createElement("div");
    k.id = item.name;
    k.className = "kachel";
    k.innerHTML = `<small></small><b></b>`;
    if (item.typ === "Switch") {
      const knopf = document.createElement("button");
      knopf.textContent = "umschalten";
      k.append(knopf);
      knopf.onclick = () => senden(item.name, "TOGGLE");
    }
    kacheln.append(k);
  }
  k.querySelector("small").textContent = item.label;
  k.querySelector("b").textContent = item.state;
  k.classList.toggle("an", item.state === "ON");
}

const senden = (name, command) =>
  fetch(`/api/items/${name}`, { method: "POST", body: command });
document.querySelector("#gutenacht").onclick = () =>
  fetch("/api/szenen/restbuch-1", { method: "POST" });

// Der Ereignisstrom des Backends: EventSource genügt, denn es braucht kein Token
const ereignisse = new EventSource("/api/ereignisse");
ereignisse.addEventListener("momentaufnahme", e => JSON.parse(e.data).forEach(zeige));
ereignisse.onmessage = e => zeige(JSON.parse(e.data));
