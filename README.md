# kotoba-robotics

[![CI](https://github.com/kotoba-lang/robotics/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/robotics/actions/workflows/ci.yml)

**Robot missions, actions and safety in pure Clojure.** The cross-cutting
[kotoba-lang](https://github.com/kotoba-lang) capability that every
`cloud-itonami-*` open business requires: all itonami verticals are designed
on the premise that a robot performs the physical domain work (drive, grasp,
pour, clean, deliver, …) under an actor that proposes actions and an
independent governor that gates them.

Models a **mission** (1 mission = 1 bounded operation; a durable outer loop
repeats missions — a single mission never loops internally), an **action**
with a safety classification, a **safety-stop**, and a **telemetry proof**
that links robot sensing to the audit ledger.

This library is **policy, not control**. It does not drive motors; it gives a
governor the records it needs to refuse unsafe actuation before it ever
reaches hardware. No network, no I/O. Portable `.cljc` across JVM /
ClojureScript / SCI / GraalVM.

## Contract

```clojure
(require '[kotoba.robotics :as rob])

(rob/mission "M1" "robot-1" "deliver parcel" :boundaries {:geo "site-A"} :max-steps 100)
(rob/action "A1" "M1" :move :low :params {:to [0 1]})
(rob/requires-sign-off? (rob/action "A1" "M1" :grasp :safety-critical)) ; => true
(rob/safety-stop "M1" :e-stop :source "operator")
(rob/telemetry-proof "M1" :lidar {:range 3.2} :timestamp "2026-07-01T00:00Z")
(rob/gate action #{:none :low})   ; => {:gate/decision :permit}
```

## Safety model

| class | actuates hardware? | sign-off? |
|---|---|---|
| `:none` / `:low` / `:medium` | varies | no |
| `:high` / `:safety-critical` | yes | **yes** (interrupt-before) |

The governor refuses any action whose safety class is not in the operator's
allowed set, and routes `:high`/`:safety-critical` to a human sign-off
(human-in-the-loop) before the action may be dispatched to hardware.

## Why

A robot performing physical work in a community (last-mile delivery, water
sampling, materials sorting, cleaning, construction) can cause real-world
harm. The single invariant — **a governor never permits an action it should
refuse, and never dispatches hardware itself** — is what makes an LLM-driven
actor safe to operate robotics in public. `kotoba-robotics` is the pure-data
layer the governor checks against; the audit ledger records every action,
hold and sign-off.

## License

Apache License 2.0.
