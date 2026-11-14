package folk.sisby.surveyor.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public interface SurveyorPacket extends CustomPacketPayload {
	int MAX_PAYLOAD_SIZE = 1_048_576;

	default List<SurveyorPacket> toPayloads() {
		return List.of(this);
	}
}
