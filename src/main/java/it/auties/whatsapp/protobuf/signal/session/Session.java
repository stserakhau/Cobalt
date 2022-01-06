package it.auties.whatsapp.protobuf.signal.session;

import lombok.NonNull;

import java.util.LinkedList;
import java.util.Optional;

public record Session(LinkedList<@NonNull SessionState> states) {
  private static final int ARCHIVED_STATES_MAX_LENGTH = 40;

  public Session(){
    this(new LinkedList<>());
  }

  public SessionState currentState(){
    return states.stream()
            .filter(state -> !state.closed())
            .findFirst()
            .orElse(null);
  }

  public Session closeCurrentState(){
    var currentState = currentState();
    if(currentState != null){
      currentState.closed(true);
    }

    return this;
  }

  public boolean hasState(int version, byte[] baseKey) {
    return states.stream()
            .anyMatch(state -> state.contentEquals(version, baseKey));
  }

  public Optional<SessionState> findState(int version, byte[] baseKey) {
    return states.stream()
            .filter(state -> state.contentEquals(version, baseKey))
            .findFirst();
  }

  public Session addState(SessionState state) {
    states.addFirst(state);
    if (states.size() > ARCHIVED_STATES_MAX_LENGTH) {
      states.removeLast();
    }

    return this;
  }
}
