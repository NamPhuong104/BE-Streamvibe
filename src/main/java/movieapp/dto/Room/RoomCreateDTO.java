package movieapp.dto.Room;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomCreateDTO {
    @Size(max = 50, message = "Tên phòng tối đa 50 ký tự")
    private String roomName;

    private String movieSlug;
    private String movieName;
    private String moviePosterUrl;
    private String movieThumbUrl;

    private boolean requireApproval = true;
}
